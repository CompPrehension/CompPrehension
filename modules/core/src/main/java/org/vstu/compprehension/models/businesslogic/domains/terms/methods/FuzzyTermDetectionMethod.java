package org.vstu.compprehension.models.businesslogic.domains.terms.methods;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermAnnotation;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDictionary;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermElement;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.TagTolerantStringTokenizer;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.WeightedSimilarity;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.vstu.compprehension.common.StringHelper.stripTags;
import static org.vstu.compprehension.models.businesslogic.domains.terms.utils.TagTolerantStringTokenizer.findWordPosition;

public class FuzzyTermDetectionMethod implements DomainTermDetectionMethod {
    
    @AllArgsConstructor
    public static class MatchResult {
        String text; int score; int pos; int length;
    }

    public static List<MatchResult> fuzzyNgramSearch(String text, String pattern, int baseMaxScore) {
        String[] words = TagTolerantStringTokenizer.tokenize(text);
        int n = TagTolerantStringTokenizer.tokenize(pattern).length;

        List<MatchResult> matches = new ArrayList<>();

        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder ngramBuilder = new StringBuilder();
            for (int j = 0; j < n; j++) {
                ngramBuilder.append(words[i + j]);
                if (j != n - 1) ngramBuilder.append(" ");
            }
            String ngram = ngramBuilder.toString();
            var posKey = Pair.of(i, i + n);

            int score = (int)(WeightedSimilarity.computeWRatio(stripTags(ngram), pattern) * 100);

            if (score >= baseMaxScore) {
                // TODO: вместо разбора текста заново взять слова из words ?
                int offset = findWordPosition(text, posKey.getKey())[0];
                int length = findWordPosition(text, posKey.getValue())[0] - offset - 1;
                if (offset != -1) {
                    matches.add(new MatchResult(ngram, score,
                            offset, length));
                }
            }
        }
        return matches;
    }

    @Override
    public DomainTermAnnotation[] findTerms(String source, DomainTermDictionary dictionary,
                                            DomainTermElement element, Language language) {
        float threshold = dictionary.getDetectionThreshold(element);
        if (threshold < 1) {
            threshold *= 100;
        }
        List<MatchResult> matches_found = fuzzyNgramSearch(source, element.getPattern(language),
                (int) threshold
        );

        // Убрать излишнюю, т.е. не присутствующую в паттерне, пунктуацию (1+ символов с конца)
        String pattern = element.getPattern(language);
        for (var match : matches_found) {
            char pattern_last_char = pattern.charAt(pattern.length() - 1);
            for (int last_pos = match.pos + match.length - 1; last_pos >= 0; --last_pos) {
                char last_char = source.charAt(last_pos);
                var c_type = Character.getType(last_char);
                if (c_type == Character.END_PUNCTUATION || c_type == Character.DASH_PUNCTUATION || c_type == Character.OTHER_PUNCTUATION) {
                    if (last_char != pattern_last_char) {
                        // Вырезать 1 символ в конце, т.к. он является неожиданной пунктуацией
                        match.length -= 1;
                        ///
                        System.out.println("\nВырезать 1 символ в конце: " +source.substring(match.pos, last_pos + 1)+ '\n');
                        ///
                        continue;
                    }
                }
                break;  // No changes, stop iteration.
            }
        }

        return matches_found.stream().sorted(Comparator.comparingInt(o -> -o.score))
                .map(match -> new DomainTermAnnotation(
                        element, language,
                        match.pos, match.length
                )).toArray(DomainTermAnnotation[]::new);
    }
}
