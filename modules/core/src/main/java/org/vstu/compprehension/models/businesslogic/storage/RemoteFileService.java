package org.vstu.compprehension.models.businesslogic.storage;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.util.Cryptor;
import org.apache.commons.vfs2.util.CryptorFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static java.lang.Math.min;


public class RemoteFileService {
    final static String PATH_SEPARATORS = "/\\#;:";
    final static String PATH_LEGAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-";
    final static String[] PATH_ILLEGAL_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",};  // in Windows
    final static int BUFFER_SIZE = 100 * 1024;

    private final FileSystemManager mgr;
    private final String baseUploadUri;

    public String getBaseUploadUri() {
        return baseUploadUri;
    }

    public String getBaseDownloadUri() {
        return baseDownloadUri;
    }

    private final String baseDownloadUri;
    private int dummyDirsForNewFile = 1;

    /**
     * Create file service with upload and download endpoints, possibly different. E.g. FTP to create files and HTTP to download them faster than ordinal FTP.
     * @param baseUploadUri URL to base dir to store data via writable protocol
     * @param baseDownloadUri URL to base dir to read data via readable protocol
     */
    RemoteFileService(String baseUploadUri, String baseDownloadUri) {
        this.baseUploadUri   = ensureTrailingSlash(baseUploadUri);
        this.baseDownloadUri = ensureTrailingSlash(baseDownloadUri);
        mgr = getFSManager();
    }

    /**
     * Create file service with endpoint for both upload and download files.
     * @param baseUri URL to base dir to store/read data via both writable and readable protocol
     */
    RemoteFileService(String baseUri) {
        this.baseUploadUri = ensureTrailingSlash(baseUri);
        // copy as not specified:
        baseDownloadUri = this.baseUploadUri;
        mgr = getFSManager();
    }

    @NotNull
    private static String ensureTrailingSlash(String baseUri) {
        // ensure '/' at the end of base path
        if (
            baseUri.charAt(baseUri.length() - 1) == '/'
                &&
            PATH_SEPARATORS.indexOf(baseUri.charAt(baseUri.length() - 2)) == -1
        ) {
            return baseUri;
        }

        while (PATH_SEPARATORS.indexOf(baseUri.charAt(baseUri.length() - 1)) != -1)
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        baseUri = baseUri + '/';
        return baseUri;
    }

    @Nullable
    private static FileSystemManager getFSManager() {
        // create generic File System manager
        FileSystemManager fsManager;
        try {
            fsManager = VFS.getManager();
            //// FileObject jarFile = fsManager.resolveFile(baseUri);
        } catch (FileSystemException e) {
            e.printStackTrace();
            fsManager = null;
            //// throw e;
        }

        return fsManager;
    }

    /** Obtain InputStream of content of specified file.
     * @param localName resource name relative to baseUri
     * @return InputStream or null if file does not exist
     * @throws FileSystemException on communication error
     */
    InputStream getFileStream(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseDownloadUri + localName);
        if (file.getType() != FileType.FILE || !file.isReadable()) {  // `!file.exists()` can be true for a directory
            return null;
        }
        FileContent fc = file.getContent();
        return fc.getInputStream(BUFFER_SIZE);
    }

    public boolean exists(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseUploadUri + localName);
        return file.exists();
    }

    /** Obtain OutputStream of specified file to write in.
     * @param localName resource name relative to baseUri
     * @return OutputStream or null if path is dir or is not writeable
     * @throws FileSystemException on communication error
     */
    public OutputStream saveFileStream(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseUploadUri + localName);
        if (file.getType() == FileType.FOLDER || !file.isWriteable()) {
            return null;
        }
        if (file.exists()) {
            // truncate file before writing ... (via deletion)
            file.delete();
        }
        FileContent fc = file.getContent();
        return fc.getOutputStream();  // `BUFFER_SIZE` does not speed up
    }

    public void deleteFile(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseUploadUri + localName);

        file.delete();
    }

    /** `When dummyDirs > 0`, for each new file of form `dir1/dir2/xxx.yyy` additional dirs inserted, e.g. `dir1/dir2/5/a/xxx.yyy` if `dummyDirs == 2`.
     * Names of additional dirs chosen randomly out of [0-9a-f] (16 hex digits).
     * @param dummyDirs number of dirs to create; set 0 to disable
     */
    public void setDummyDirsForNewFile(int dummyDirs) {
        this.dummyDirsForNewFile = dummyDirs;
    }

    public String insertDummyDirs(String filepath) {
        if (dummyDirsForNewFile <= 0)
            return filepath;


        long hash = getCRC32Checksum(filepath.getBytes(StandardCharsets.UTF_8));

        StringBuilder dummy = new StringBuilder();
        for (int i = 0; i < dummyDirsForNewFile && hash > 0; ++i) {
            String folderName = String.format("%x", hash & 0x0f);
            hash >>= 4;  // 0x0f is 4 bits
            dummy.append(folderName).append("/");
        }

        int slashIndex = filepath.lastIndexOf('/');

        return filepath.substring(0, slashIndex + 1) + dummy + filepath.substring(slashIndex + 1);
    }

    /**
     * Change name in case of clash with existing names in FS
     * @param name local file name (or relative path) to check
     * @return name corrected if necessary
     */
    public String prepareNameForFile(String name, boolean makeUnique) {
        try {
            name = normalizeName(name);
            name = insertDummyDirs(name);
            if (makeUnique) {
                name = uniqualizeName(name);
            }
            return name;
        } catch (FileSystemException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Change name in case of clash with existing names in FS
     * @param normalizedName local file name (or relative path) to check
     * @return name corrected if necessary
     * @throws FileSystemException on FS communication error
     */
    public String uniqualizeName(String normalizedName) throws FileSystemException {
        if (!exists(normalizedName)) {
            return normalizedName;
        }

        String name = normalizedName;
        String ext = "";
        int dotIndex = normalizedName.lastIndexOf('.');
        if (dotIndex > -1) {
            name = normalizedName.substring(0, dotIndex);
            ext = normalizedName.substring(dotIndex);
        }
        // find digits suffix position
        int underscoreIndex = name.lastIndexOf('_');
        if (underscoreIndex > -1) {
            for (char h : name.substring(min(name.length(), underscoreIndex + 1)).toCharArray()) {
                if (!Character.isDigit(h)) {
                    underscoreIndex = name.length();
                    break;
                }
            }
        }

        int counter = 1;
        try {
            counter = Integer.parseInt(name.substring(min(name.length(), underscoreIndex + 1)));
        } catch (NumberFormatException ignored) {}

        do {
            counter += 1;
            normalizedName = name.substring(0, underscoreIndex) + "_" + counter + ext;
        } while (exists(normalizedName));

        return normalizedName;
    }

    /** Prepare valid (sub)path from arbitrary string, preserving latin letters, digits and "._-"
     * @param p arbitrary string
     * @return prepared valid (sub)path
     */
    public static String normalizeName(String p) {
        StringBuilder whole = new StringBuilder(p.length());
        StringBuilder word = new StringBuilder(p.length() / 2);
        boolean previousNormal = true;
        for (char h : p.toCharArray()) {
            if (PATH_SEPARATORS.indexOf(h) != -1) {
                if (word.length() > 0) {
                    if (Arrays.stream(PATH_ILLEGAL_NAMES).anyMatch(s -> s.equalsIgnoreCase(word.toString()))) {
                        word.append("_"); // avoid illegal names in Windows
                    }
                    whole.append(word).append("/");
                    word.delete(0, word.length());  // clear word
                }
                continue;
            }
            if (PATH_LEGAL_CHARS.indexOf(h) == -1) {
                if (previousNormal) {
                    word.append("_");
                }
                previousNormal = false;
            } else {
                word.append(h);
                previousNormal = true;
            }
        }
        if (word.length() > 0) {
            if (Arrays.stream(PATH_ILLEGAL_NAMES).anyMatch(s -> s.equalsIgnoreCase(word.toString()))) {
                word.append("_"); // avoid illegal names in Windows
            }
            whole.append(word);
        }
        return whole.toString();
    }

    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    /**
     * Prepare connection string with optionally encrypted password to cut from output and save elsewhere
     */
    private static String ftpUriString(String user, String pass, String domain, boolean encryptPassword) {
        // See: https://commons.apache.org/proper/commons-vfs/filesystems.html
        // URI Format
        // ftp://[ username[: password]@] hostname[: port][ relative-path]
        //
        // Examples:
        // ftp://myusername:mypassword@somehost/pub/downloads/somefile.tgz
        // https://testuser:{D7B82198B272F5C93790FEB38A73C7B8}@myhost.com/svn/repos/vfstest/trunk
        if (encryptPassword) {
            try {
                pass = "{" + encryptPassword(pass) + "}";
            } catch (Exception e) {
                System.err.println("Cannot create encrypt password: " + pass);
                e.printStackTrace();
            }
        }
        return String.format("ftp://%s:%s@%s", user, pass, domain);
    }

    private static String encryptPassword(String p) throws Exception {
        Cryptor cryptor = CryptorFactory.getCryptor();
        return cryptor.encrypt(p);
    }



    public static void main(String[] args) throws IOException {

        // Prepare connection string with optionally encrypted password: cut from output and save elsewhere
        String s = ftpUriString("poas", "your-password-here", "vds84.server-1.biz/ftp_dir", true);
        System.out.println(s);

////        String connectionStr = "file:///c:/Temp2/data/";
//        String connectionStr = "ftp://poas:{6689596D2347FA1287A4FD6AB36AA9C8}@vds84.server-1.biz/ftp_dir/compp/tmp";
//        String connectionDownloadStr = "http://vds84.server-1.biz/misc/ftp/compp/tmp/";
//        RemoteFileService rfs = new RemoteFileService(connectionStr, connectionDownloadStr);
//        rfs.test();
    }

    private void test() throws IOException {
        assert mgr != null;

//        setDummyDirsForNewFile(3);
//        System.out.println(insertDummyDirs("file1.txt"));
//        System.out.println(insertDummyDirs("mydir/file1.txt"));
//        System.out.println(insertDummyDirs("my/dir/file1.txt"));

        // test uploads / downloads
        for (int i = 0 ; i < 3 ; i++) {
            long startTime = System.nanoTime();

//            try(OutputStream stream = saveFileStream("dump.bin")) {
//                for (int k = 0 ; k < 1000 ; k++) {
//                    stream.write(new byte[100]);
//                }
//            }  // 1,7 s

            try(InputStream stream = getFileStream("dump.bin")) {
                while (stream.read(new byte[100]) > 0) {}
            }  // 0.98 s via FTP ;  ~ 0.45 s via HTTP (Nginx)
               // 0.98 s via FTP ;  ~ 0.20 s via HTTP (fast 4G)

            long estimatedTime = System.nanoTime() - startTime;
            System.out.println("Take "+ i +": " + String.format("%.5f", (float)estimatedTime / 1000 / 1000 / 1000) + " seconds.");
        }


//        final FileObject file = mgr.resolveFile(baseUri + "compp/a/test.abc");
//
//        if (!file.exists()) {
//            file.createFile();  // does `make dirs` automatically.
//            System.out.println("file created!");
//        }
//
//        FileContent fc = file.getContent();
//
//        byte[] b = new byte[2];
//        InputStream in = fc.getInputStream();
//        int nread = in.read(b);
//        System.out.println(nread);
//        System.out.println(Arrays.toString(b));

//        fc.getOutputStream().write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe});
//        fc.close();


//        System.out.println("URL: " + file.getURL());
//        System.out.println("getName(): " + file.getName());
//        System.out.println("BaseName: " + file.getName().getBaseName());
//        System.out.println("Extension: " + file.getName().getExtension());
//        System.out.println("Path: " + file.getName().getPath());
//        System.out.println("Scheme: " + file.getName().getScheme());
//        System.out.println("URI: " + file.getName().getURI());
//        System.out.println("Root URI: " + file.getName().getRootURI());
//        System.out.println("Parent: " + file.getName().getParent());
//        System.out.println("Type: " + file.getType());
//        System.out.println("Exists: " + file.exists());
//        System.out.println("Readable: " + file.isReadable());
//        System.out.println("Writeable: " + file.isWriteable());
//        System.out.println("Root path: " + file.getFileSystem().getRoot().getName().getPath());

        /*
        URL: file:///c:/Temp2/data/a/b/file.tst
        getName(): file:///c:/Temp2/data/a/b/file.tst
        BaseName: file.tst
        Extension: tst
        Path: /Temp2/data/a/b/file.tst
        Scheme: file
        URI: file:///c:/Temp2/data/a/b/file.tst
        Root URI: file:///c:/
        Parent: file:///c:/Temp2/data/a/b
        Type: imaginary
        Exists: false
        Readable: false
        Writeable: true
        Root path: /
         */
    }
}
