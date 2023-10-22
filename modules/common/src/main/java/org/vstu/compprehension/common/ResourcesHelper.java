package org.vstu.compprehension.common;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Log4j2
public class ResourcesHelper {
    public static String ensureFolderExtracted(Class<?> packageMarkerType, String resourceFolderName, String targetFolderName) {
        return ensureFolderExtracted(packageMarkerType, resourceFolderName, targetFolderName, false);
    }

    public static String ensureFolderExtracted(Class<?> packageMarkerType, String resourceFolderName, String targetFolderName, boolean override) {
        log.debug("Trying to ensure resource folder extracted");

        var currentExecutable = ClassLocationHelper.urlToFile(ClassLocationHelper.getLocation(packageMarkerType));
        var currentExecutableFolder = currentExecutable.isDirectory() ? currentExecutable.toString() : currentExecutable.getParentFile().toString();

        if (currentExecutable.isDirectory()) {
            log.info("Found directory environment");
            return FileHelper.combinePaths(currentExecutableFolder, resourceFolderName).toString();
        }
        if (currentExecutable.getName().endsWith(".jar")) {
            log.info("Found jar environment: {}", currentExecutable.getName());

            var extractConfigurationPath = Paths.get(targetFolderName).toAbsolutePath();
            log.debug("out folder path: {}", extractConfigurationPath);

            if (!override && Files.exists(extractConfigurationPath)) {
                log.debug("no additional extraction needed");
                return extractConfigurationPath.toString();
            }

            log.info("Found jar environment: {}", currentExecutable.getName());
            extractFolderFromJar(packageMarkerType, resourceFolderName, currentExecutable, extractConfigurationPath);

            return extractConfigurationPath.toString();
        }
        throw new RuntimeException("Unsupported packaging type");
    }

    @SneakyThrows
    private static void extractFolderFromJar(Class<?> packageMarkerType, String folderName, File currentExecutable, Path extractConfigurationPath) {
        try {
            var jarPath = currentExecutable.toString().replace('\\', '/');
            var jarName = currentExecutable.getName();
            log.debug("normalized jar name: {}", jarName);

            extractConfigurationPath = extractConfigurationPath.toAbsolutePath();

            var folderPathInResources = getFolderFullPathInJar(packageMarkerType, jarPath, folderName);
            log.debug("full path to resources in jar: {}", folderPathInResources);
            var folderPathFragments = folderPathInResources.split("!");

            // проходим по всем вложенным jar'ам
            var nestedJars = new ArrayList<JarInfo>(folderPathFragments.length - 1);
            var currentJar = new JarFile(folderPathFragments[0]);
            var currentJarPath = currentExecutable.toPath().toAbsolutePath();
            nestedJars.add(new JarInfo(currentJar, currentJarPath));
            for (int i = 1; i < folderPathFragments.length; ++i) {
                var fragment = folderPathFragments[i];
                if (fragment.startsWith("/"))
                    fragment = fragment.substring(1);

                if (fragment.endsWith(".jar")) {
                    JarEntry nestedJarEntry = currentJar.getJarEntry(fragment);
                    currentJarPath = Path.of(extractConfigurationPath.toAbsolutePath().toString(), Path.of(nestedJarEntry.getName()).getFileName().toString());
                    currentJar = new JarFile(new File(extract(currentJar.getInputStream(nestedJarEntry), currentJarPath.toString())));
                    nestedJars.add(new JarInfo(currentJar, currentJarPath));
                } else {
                    Enumeration<JarEntry> entries = currentJar.entries();
                    var folderNameWithSlash = folderName + "/";
                    while (entries.hasMoreElements()) {
                        var desiredEntry = entries.nextElement();
                        if (desiredEntry.getName().startsWith(folderNameWithSlash) && !desiredEntry.isDirectory()) {
                            var outFolderPath = extractConfigurationPath.toAbsolutePath().toString();
                            var pathToEntryInsideJarFolder = desiredEntry.getName().substring(folderNameWithSlash.length());
                            var outFilePath = Path.of(outFolderPath, pathToEntryInsideJarFolder).toAbsolutePath().toString();
                            extract(currentJar.getInputStream(desiredEntry), outFilePath);
                            log.debug("Extracted file: {}", outFilePath);
                        }
                    }
                }
            }
            for (int i = 0; i < nestedJars.size(); i++) {
                var jar = nestedJars.get(i);
                jar.file.close();
                if (i >= 1) {
                    Files.delete(jar.path);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Не удалось извлечь папку c конфигурацией: [%s], из jar файла", folderName), e);
        }
    }

    private record JarInfo(JarFile file, Path path) {};

    private static String getFolderFullPathInJar(Class<?> packageMarkerType, String jarPath, String folderName) {
        var folderPathInJar = Optional.ofNullable(packageMarkerType.getClassLoader().getResource(folderName))
                .map(URL::getPath)
                .orElseThrow(() -> new RuntimeException(String.format("Невозможно найти папку %s в ресурсах сборки", folderName)));
        folderPathInJar = jarPath.startsWith("/") ? folderPathInJar.replaceAll("file:", "") : folderPathInJar.replaceAll("file:/", "");
        if (folderPathInJar.startsWith("/")) {
            folderPathInJar = folderPathInJar.substring(1);
        }
        return folderPathInJar;
    }

    private static String extract(InputStream in, String outputFile) throws IOException {
        File file = new File(outputFile);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileOutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        return file.getAbsolutePath();
    }
}
