package org.vstu.compprehension.models.businesslogic.domains.terms.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagTolerantStringTokenizer {
    // Регулярное выражение для тега вида <...>
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^<>]*>");

    public static String[] tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '<') {
                Matcher tagMatcher = TAG_PATTERN.matcher(input);
                tagMatcher.region(i, input.length());
                if (tagMatcher.lookingAt()) {
                    String tag = tagMatcher.group();
                    current.append(tag);
                    i += tag.length();
                } else {
                    // Просто символ '<'
                    if (Character.isWhitespace(c)) {
                        if (current.length() > 0) {
                            tokens.add(current.toString());
                            current.setLength(0);
                        }
                    } else {
                        current.append(c);
                    }
                    i++;
                }
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens.toArray(new String[0]);
    }

    public static int[] findWordPosition(String text, int wordIndex) {
        if (wordIndex < 0 || text == null) {
            return null;
        }
        int pos = 0;
        int currentToken = 0;
        while (currentToken <= wordIndex && pos < text.length()) {
            int start = pos;
            StringBuilder tokenBuilder = new StringBuilder();
            while (pos < text.length()) {
                if (text.charAt(pos) == '<') {
                    Matcher tagMatcher = TAG_PATTERN.matcher(text);
                    tagMatcher.region(pos, text.length());
                    if (tagMatcher.lookingAt()) {
                        String tag = tagMatcher.group();
                        tokenBuilder.append(tag);
                        pos += tag.length();
                        continue;
                    }
                }
                if (Character.isWhitespace(text.charAt(pos))) {
                    if (tokenBuilder.length() > 0) {
                        break;
                    }
                    start++;
                    pos++;
                } else {
                    tokenBuilder.append(text.charAt(pos));
                    pos++;
                }
            }

            if (currentToken == wordIndex) {
                return new int[]{start, pos};
            }
            currentToken++;
        }

        if (wordIndex >= currentToken) {
            return new int[] {text.length() + 1, text.length() + 1};
        }

        return new int[]{-1, -1};
    }
}