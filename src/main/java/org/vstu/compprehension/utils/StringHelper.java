package org.vstu.compprehension.utils;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class StringHelper {
    public static boolean startsWithIgnoreCase(String source, String target) {
        return source.regionMatches(true, 0, target, 0, target.length());
    }

    public static String joinWithDelimiter(@NotNull String delim, Object... parts) {
        val joiner = new StringJoiner(delim);
        for (val part : parts) {
            joiner.add(part == null ? null : part.toString());
        }
        return joiner.toString();
    }

    public static String joinWithSpace(Object... parts) {
        return joinWithDelimiter(" ", parts);
    }
}
