package org.vstu.compprehension.utils;

import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public static int findCommonPrefixLength(@Nullable String s1, @Nullable String s2) {
        if (s1 == null || s2 == null)
            return 0;
        int maxLength = Math.min(s1.length(), s2.length());
        var length = 0;
        while (length < maxLength && s1.charAt(length) == s2.charAt(length)) {
            ++length;
        }
        return length;
    }

    public static String joinWithSpace(Object... parts) {
        return joinWithDelimiter(" ", parts);
    }
}
