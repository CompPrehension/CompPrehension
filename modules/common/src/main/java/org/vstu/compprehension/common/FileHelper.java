package org.vstu.compprehension.common;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class FileHelper {
    public static Path combinePaths(String firstPath, String... otherPaths) {
        var file = new File(firstPath);
        for (String otherPath : otherPaths) {
            file = new File(file, otherPath);
        }
        return file.toPath().normalize();
    }


    public static Optional<Path> tryCombinePaths(String firstPath, String... otherPaths) {
        try {
            return Optional.of(combinePaths(firstPath, otherPaths));
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files != null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static void deleteFolderContent(File folder) {
        File[] files = folder.listFiles();
        if(files != null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }
}
