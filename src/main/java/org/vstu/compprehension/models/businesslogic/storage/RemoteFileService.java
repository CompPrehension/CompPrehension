package org.vstu.compprehension.models.businesslogic.storage;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.util.Cryptor;
import org.apache.commons.vfs2.util.CryptorFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static java.lang.Math.min;


public class RemoteFileService {
    final static String PATH_SEPARATORS = "/\\#;:";
    final static String PATH_LEGAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-";
    final static String[] PATH_ILLEGAL_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",};  // in Windows

    private final FileSystemManager mgr;
    private final String baseUri;

    RemoteFileService(String baseUri) {
        // ensure '/' at the end of base path
        while (PATH_SEPARATORS.indexOf(baseUri.charAt(baseUri.length() - 1)) != -1)
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        this.baseUri = baseUri + '/';

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

        mgr = fsManager;
    }

    /** Obtain InputStream of content of specified file.
     * @param localName resource name relative to baseUri
     * @return InputStream or null if file does not exist
     * @throws FileSystemException on communication error
     */
    InputStream getFileStream(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseUri + localName);
        if (file.getType() != FileType.FILE || !file.isReadable()) {  // `!file.exists()` can be true for a directory
            return null;
        }
        FileContent fc = file.getContent();
        return fc.getInputStream();
    }

    public boolean exists(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseUri + localName);
        return file.exists();
    }

    /** Obtain OutputStream of specified file to write in.
     * @param localName resource name relative to baseUri
     * @return OutputStream or null if path is dir or is not writeable
     * @throws FileSystemException on communication error
     */
    OutputStream saveFileStream(String localName) throws FileSystemException {
        final FileObject file = mgr.resolveFile(baseUri + localName);
        if (file.getType() == FileType.FOLDER || !file.isWriteable()) {
            return null;
        }
        FileContent fc = file.getContent();
        return fc.getOutputStream();
    }

    /**
     * Change name in case of clash with existing names in FS
     * @param name local file name (or relative path) to check
     * @return name corrected if necessary
     * @throws FileSystemException on FS communication error
     */
    public String prepareNameForNewFile(String name) {
        try {
            return uniqualizeName(normalizeName(name));
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
//        String connectionStr = "ftp://poas:{6689596D2347FA1287A4FD6AB36AA9C8}@vds84.server-1.biz/ftp_dir/";
//        RemoteFileService rfs = new RemoteFileService(connectionStr);
//        rfs.test();
    }

    private void test() throws IOException {
        assert mgr != null;

        String s = "1__memcpy_s";
        String s2 = normalizeName(s);
        System.out.println(s2);

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
