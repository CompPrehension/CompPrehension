package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.util.Cryptor;
import org.apache.commons.vfs2.util.CryptorFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static java.lang.Math.min;

@Log4j2
public class RemoteFileService {
    final static String PATH_SEPARATORS = "/\\#;:";
    final static String PATH_LEGAL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-";
    final static String[] PATH_ILLEGAL_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",};  // in Windows
    final static int BUFFER_SIZE = 100 * 1024;

    private FileSystemManager mgr;
    private final URI baseUploadUri;

    private final URI baseDownloadUri;
    private int dummyDirsForNewFile = 1;

    /**
     * Create file service with upload and download endpoints, possibly different. E.g. FTP to create files and HTTP to download them faster than ordinal FTP.
     * @param baseUploadUri URL to base dir to store data via writable protocol
     * @param baseDownloadUri URL to base dir to read data via readable protocol
     */
    RemoteFileService(String baseUploadUri, String baseDownloadUri) throws URISyntaxException {
        this.baseUploadUri   = new URI(ensureTrailingSlash(baseUploadUri));
        this.baseDownloadUri = new URI(ensureTrailingSlash(baseDownloadUri));
        mgr = getFSManager();
    }

    public RemoteFileService(String baseUploadUri, String baseDownloadUri, int dummyDirsForNewFile) throws URISyntaxException {
        this(baseUploadUri, baseDownloadUri);
        this.dummyDirsForNewFile = dummyDirsForNewFile;
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
            log.error("Error creation FS manager - {}", e.getMessage(), e);
            fsManager = null;
            //// throw e;
        }

        return fsManager;
    }

    /** return initiailized File System manager */
    private FileSystemManager getMgr() {
        if (mgr == null) {
            mgr = getFSManager();
        }
        return mgr;
    }

    /** Close file system resources.
     * @see <a href="https://cwiki.apache.org/confluence/display/commons/VfsFaq">How do I keep an SFTP connection from hanging?</a>
     * */
    public void closeConnections() throws FileSystemException {
        if (mgr != null) {
            if (mgr instanceof DefaultFileSystemManager) {
                DefaultFileSystemManager m = (DefaultFileSystemManager) mgr;
                m.close();
                m.init();
            } else
                mgr.close();
                // Note: no `init()` available in `mgr` here.
        }
        mgr = null;
    }


    /** Obtain InputStream of content of specified file.
     * @param localName resource name relative to baseUri
     * @return InputStream or null if file does not exist
     * @throws FileSystemException on communication error
     */
    InputStream getFileStream(String localName) throws FileSystemException {
        final FileObject file = getMgr().resolveFile(baseDownloadUri + localName);
        if (file.getType() != FileType.FILE || !file.isReadable()) {  // `!file.exists()` can be true for a directory
            return null;
        }
        FileContent fc = file.getContent();
        return fc.getInputStream(BUFFER_SIZE);
    }

    public boolean exists(String localName) throws FileSystemException {
        final FileObject file = getMgr().resolveFile(baseUploadUri + localName);
        return file.exists();
    }

    private URI resolveRelativePath(String relativePath) {
        return baseUploadUri.resolve(relativePath);
    }

    public @Nullable OutputStream openForWrite(String relativePath) throws FileSystemException  {
        var fileUri = resolveRelativePath(relativePath);
        return openForWrite(fileUri);
    }

    private @Nullable OutputStream openForWrite(URI path) throws FileSystemException  {
        final FileObject file = getMgr().resolveFile(path);
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
        final FileObject file = getMgr().resolveFile(baseUploadUri + localName);

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
            log.error("Error preparing name for file - {}", e.getMessage(), e);
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
                log.error("Error encrypting password - {}", e.getMessage(), e);
            }
        }
        return String.format("ftp://%s:%s@%s", user, pass, domain);
    }

    private static String encryptPassword(String p) throws Exception {
        Cryptor cryptor = CryptorFactory.getCryptor();
        return cryptor.encrypt(p);
    }
}
