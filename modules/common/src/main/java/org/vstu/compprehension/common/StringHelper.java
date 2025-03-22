package org.vstu.compprehension.common;

import lombok.val;
import org.jetbrains.annotations.Contract;
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

    @Contract("null -> null")
    public static Integer tryParseInt(@Nullable String s) {
        if (s == null)
            return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String joinWithSpace(Object... parts) {
        return joinWithDelimiter(" ", parts);
    }

    @Contract("null -> true")
    public static boolean isNullOrEmpty(@Nullable String s) {
        return s == null || s.length() == 0;
    }

    @Contract("null -> true")
    public static boolean isNullOrWhitespace(@Nullable String s) {
        return s == null || s.length() == 0 || isWhitespace(s);
    }

    private static boolean isWhitespace(String s) {
        int length = s.length();
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                if (!Character.isWhitespace(s.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
