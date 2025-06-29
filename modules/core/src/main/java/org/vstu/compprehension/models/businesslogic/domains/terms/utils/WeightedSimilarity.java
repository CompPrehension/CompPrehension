package org.vstu.compprehension.models.businesslogic.domains.terms.utils;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedSimilarity {

    private static final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public static double computeWRatio(String s1, String s2) {
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();

        if (s1.isEmpty() || s2.isEmpty()) return 0;

        double base = basicRatio(s1, s2);
        double partial = partialRatio(s1, s2);
        double tokenSort = tokenSortRatio(s1, s2);
        double tokenSet = tokenSetRatio(s1, s2);

        return Collections.max(List.of(
                base,
                partial * 0.9,
                tokenSort * 0.95,
                tokenSet * 0.95
        )) / 100.0;
    }

    private static double basicRatio(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 100.0;
        int dist = levenshtein.apply(s1, s2);
        return 100.0 * (1 - (double) dist / maxLen);
    }

    private static double partialRatio(String s1, String s2) {
        if (s1.length() <= 2 || s2.length() <= 2) return 0;
        if (s1.length() > s2.length()) {
            return partialRatio(s2, s1);
        }
        int maxScore = 0;
        for (int i = 0; i <= s2.length() - s1.length(); i++) {
            String sub = s2.substring(i, i + s1.length());
            int dist = levenshtein.apply(s1, sub);
            int score = (int) (100.0 * (1 - (double) dist / s1.length()));
            maxScore = Math.max(maxScore, score);
        }
        return maxScore;
    }

    private static double tokenSortRatio(String s1, String s2) {
        String sorted1 = sortTokens(s1);
        String sorted2 = sortTokens(s2);
        return basicRatio(sorted1, sorted2);
    }

    private static double tokenSetRatio(String s1, String s2) {
        Set<String> set1 = new HashSet<>(List.of(s1.split("\\s+")));
        Set<String> set2 = new HashSet<>(List.of(s2.split("\\s+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> diff1 = new HashSet<>(set1);
        diff1.removeAll(set2);

        Set<String> diff2 = new HashSet<>(set2);
        diff2.removeAll(set1);

        String partial1 = sortTokens(String.join(" ", intersection) + " " + String.join(" ", diff1));
        String partial2 = sortTokens(String.join(" ", intersection) + " " + String.join(" ", diff2));

        return basicRatio(partial1, partial2);
    }

    private static String sortTokens(String s) {
        return Arrays.stream(s.trim().split("\\s+")).sorted().collect(Collectors.joining(" "));
    }
}
