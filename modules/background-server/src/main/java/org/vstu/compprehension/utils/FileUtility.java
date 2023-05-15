package org.vstu.compprehension.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtility {
    public static List<String> findFiles(Path path, String[] fileExtensions) throws IOException {

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path must be a directory!");
        }

        List<String> result;
        try (Stream<Path> walk = Files.walk(path, 25)) {
            result = walk
                    .filter(p -> !Files.isDirectory(p))
                    // convert path to string
                    .map(p -> p.toString()/*.toLowerCase()*/)
                    .filter(f -> isEndWith(f.toLowerCase(), fileExtensions))
                    .collect(Collectors.toList());
        }
        return result;

    }

    public static int MAX_CMD_LENGTH = 32768;  // in Windows

    /**
     * @param cmdLineParts non-breakable parts on command line
     * @param followingPartsLength keep "free space" of so many chars for any potential parts added to this command line later
     * @return first parts of the command line whose cumulative length does not exceed the length limit
     */
    @NotNull
    public static List<String> truncateLongCommandline(@NotNull List<String> cmdLineParts, int followingPartsLength) {
        int limit = MAX_CMD_LENGTH - followingPartsLength;
        if (cmdLineParts.stream().mapToInt(String::length).sum() + cmdLineParts.size() < limit)
            // ok: not too long
            return cmdLineParts;

        int includeNParts = 0;
        int accumulatedLength = 0;
        for (String part : cmdLineParts) {
            accumulatedLength += 1 + part.length();
            if (accumulatedLength >= limit)
                break;
            includeNParts++;
        }
        return new ArrayList<>(cmdLineParts.subList(0, includeNParts));
    }

    private static boolean isEndWith(String file, String[] fileExtensions) {
        boolean result = false;
        for (String fileExtension : fileExtensions) {
            if (file.endsWith(fileExtension)) {
                result = true;
                break;
            }
        }
        return result;
    }

}
