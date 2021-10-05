package org.vstu.compprehension.utils;

public class StringHelper {
    public static boolean startsWithIgnoreCase(String source, String target) {
        return source.regionMatches(true, 0, target, 0, target.length());
    }
}
