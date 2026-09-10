package org.vstu.compprehension.businesslogic.domains.helpers;

import its.model.DirectoryScanUtils;
import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.loqi.DomainLoqiBuilder;
import its.model.definition.loqi.TreeLoqiBuilder;
import its.model.nodes.DecisionTree;
import its.model.nodes.xml.DecisionTreeXMLBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DomainSolvingModelLoader {
    private static final Pattern TAG_FILE = Pattern.compile("tag_(\\S+)\\.loqi");
    private static final Pattern TREE_XML_FILE = Pattern.compile("tree(_\\S+|)\\.xml");
    private static final Pattern TREE_LOQI_FILE = Pattern.compile("tree(_\\S+|)\\.loqi");

    private DomainSolvingModelLoader() {
    }

    public static DomainSolvingModel load(URL directoryUrl, DomainSolvingModel.BuildMethod buildMethod) {
        var directory = withTrailingSlash(Objects.requireNonNull(directoryUrl, "domain model directory url is null"));
        var fileNames = listFileNames(directory);
        var domainModel = DomainSolvingModel.collectDomain(directory, buildMethod);
        var tags = buildMethod == DomainSolvingModel.BuildMethod.LOQI
                ? collectTags(directory, fileNames)
                : Map.<String, DomainModel>of();
        return new DomainSolvingModel(domainModel, tags, collectTrees(directory, fileNames));
    }

    public static DomainSolvingModel loadFromClasspath(ClassLoader classLoader, String directory, DomainSolvingModel.BuildMethod buildMethod) {
        var directoryUrl = classLoader.getResource(directory);
        if (directoryUrl == null) {
            throw new IllegalStateException("Domain model directory '" + directory + "' is not found in classpath");
        }
        return load(directoryUrl, buildMethod);
    }

    private static Map<String, DomainModel> collectTags(URL directory, List<String> fileNames) {
        var tags = new LinkedHashMap<String, DomainModel>();
        for (var fileName : fileNames) {
            var match = TAG_FILE.matcher(fileName);
            if (!match.matches()) {
                continue;
            }
            try (Reader reader = openReader(child(directory, fileName))) {
                tags.put(match.group(1), DomainLoqiBuilder.buildDomain(reader));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read domain model tag file '" + fileName + "' from " + directory, e);
            }
        }
        return tags;
    }

    private static Map<String, DecisionTree> collectTrees(URL directory, List<String> fileNames) {
        var trees = new LinkedHashMap<String, DecisionTree>();
        for (var fileName : fileNames) {
            var xmlMatch = TREE_XML_FILE.matcher(fileName);
            if (xmlMatch.matches()) {
                trees.put(treeName(xmlMatch), readXmlTree(directory, fileName));
                continue;
            }
            var loqiMatch = TREE_LOQI_FILE.matcher(fileName);
            if (loqiMatch.matches()) {
                trees.put(treeName(loqiMatch), readLoqiTree(directory, fileName));
            }
        }
        return trees;
    }

    private static DecisionTree readXmlTree(URL directory, String fileName) {
        var file = child(directory, fileName);
        try (Reader reader = openReader(file)) {
            var content = new StringBuilder();
            var buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                content.append(buffer, 0, read);
            }
            return DecisionTreeXMLBuilder.fromXMLString(content.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read decision tree file '" + fileName + "' from " + directory, e);
        }
    }

    private static DecisionTree readLoqiTree(URL directory, String fileName) {
        var file = child(directory, fileName);
        try (Reader reader = openReader(file)) {
            return TreeLoqiBuilder.buildTree(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read decision tree file '" + fileName + "' from " + directory, e);
        }
    }

    private static String treeName(Matcher match) {
        var name = match.group(1);
        return name.startsWith("_") ? name.substring(1) : name;
    }

    private static List<String> listFileNames(URL directory) {
        try {
            return switch (directory.getProtocol()) {
                case "file" -> listDirectoryFileNames(directory);
                case "jar" -> listJarFileNames(directory);
                default -> DirectoryScanUtils.findFiles(directory).stream()
                        .map(DirectoryScanUtils.FileDescription::getFilename)
                        .toList();
            };
        } catch (Exception e) {
            throw new IllegalStateException("Cannot list files of domain model directory " + directory, e);
        }
    }

    private static List<String> listDirectoryFileNames(URL directory) throws Exception {
        var files = new File(directory.toURI()).listFiles();
        if (files == null) {
            return List.of();
        }
        var names = new ArrayList<String>();
        for (var file : files) {
            if (file.isFile()) {
                names.add(file.getName());
            }
        }
        return names;
    }

    private static List<String> listJarFileNames(URL directory) throws IOException {
        URLConnection connection = directory.openConnection();
        if (!(connection instanceof JarURLConnection jarConnection)) {
            throw new IllegalStateException("Unsupported domain model directory url: " + directory);
        }
        jarConnection.setUseCaches(true);
        JarFile jarFile = jarConnection.getJarFile();
        var prefix = jarConnection.getEntryName();
        if (prefix == null) {
            return List.of();
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }

        var names = new ArrayList<String>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            var entryName = entry.getName();
            if (!entryName.startsWith(prefix)) {
                continue;
            }
            var relativeName = entryName.substring(prefix.length());
            if (relativeName.isEmpty() || relativeName.contains("/")) {
                continue;
            }
            names.add(relativeName);
        }
        return names;
    }

    private static Reader openReader(URL file) throws IOException {
        return new BufferedReader(new InputStreamReader(file.openStream(), StandardCharsets.UTF_8));
    }

    private static URL child(URL directory, String fileName) {
        try {
            return new URL(directory.getProtocol(), directory.getHost(), directory.getPort(), directory.getPath() + fileName);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot resolve '" + fileName + "' in " + directory, e);
        }
    }

    private static URL withTrailingSlash(URL directory) {
        if (directory.toString().endsWith("/")) {
            return directory;
        }
        try {
            return new URL(directory.getProtocol(), directory.getHost(), directory.getPort(), directory.getPath() + "/");
        } catch (IOException e) {
            throw new IllegalStateException("Invalid domain model directory url: " + directory, e);
        }
    }
}
