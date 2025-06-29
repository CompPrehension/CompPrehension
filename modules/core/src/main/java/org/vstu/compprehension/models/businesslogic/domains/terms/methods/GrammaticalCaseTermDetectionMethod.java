package org.vstu.compprehension.models.businesslogic.domains.terms.methods;

import its.questions.gen.formulations.Case;
import its.questions.gen.formulations.TemplatingUtils;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermAnnotation;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDictionary;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermElement;
import org.vstu.compprehension.models.businesslogic.domains.terms.utils.TagTolerantStringTokenizer;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.vstu.compprehension.common.StringHelper.findWordPosition;
import static org.vstu.compprehension.common.StringHelper.stripTags;

public class GrammaticalCaseTermDetectionMethod implements DomainTermDetectionMethod {

    public record MatchResult(String text, int pos, int length) { }

    // Все 6 падежей
    private static final Case[] CASES = Case.values();

    public static List<MatchResult> caseAwareNgramSearch(String text, String pattern) {
        String[] words = TagTolerantStringTokenizer.tokenize(text);
        String[] patternWords = pattern.split("\\s+");
        int n = patternWords.length;

        List<MatchResult> matches = new ArrayList<>();

        // Для каждой позиции возможного n-грамма в тексте
        for (int i = 0; i <= words.length - n; i++) {
            String[] ngram = Arrays.copyOfRange(words, i, i + n);

            // Проверяем все варианты падежей для слов шаблона
            if (matchesInAnyCase(ngram, patternWords)) {
                int startOffset = findWordPosition(text, 0)[0];
                int endOffset = findWordPosition(text, n - 1)[0] + ngram[n - 1].length();
                int length = endOffset - startOffset - 1;

                matches.add(new MatchResult(String.join(" ", ngram), startOffset, length));
            }
        }
        return matches;
    }

    private static boolean matchesInAnyCase(String[] ngram, String[] patternWords) {
        // Перебор падежей (общий для всех слов)
        for (Case grammaticalCase : CASES) {
            boolean allMatch = true;

            for (int j = 0; j < patternWords.length; j++) {
                String patternWord = patternWords[j];
                String ngramWord = stripTags(ngram[j]);

                // Получение всех возможных форм слова шаблона
                String patternForm = TemplatingUtils.toCase(patternWord, grammaticalCase);
                if (!ngramWord.equalsIgnoreCase(patternForm)) {
                    allMatch = false;
                    break;
                }
            }

            if (allMatch) return true;
        }
        return false;
    }

    @Override
    public DomainTermAnnotation[] findTerms(String source, DomainTermDictionary dictionary,
                                            DomainTermElement element, Language language) {
        return caseAwareNgramSearch(source, element.getPattern(language))
                .stream()
                .map(match -> new DomainTermAnnotation(
                        element, language,
                        match.pos(),
                        match.length()
                ))
                .toArray(DomainTermAnnotation[]::new);
    }
}
