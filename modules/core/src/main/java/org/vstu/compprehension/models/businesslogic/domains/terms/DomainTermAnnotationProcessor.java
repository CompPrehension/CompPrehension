package org.vstu.compprehension.models.businesslogic.domains.terms;

import lombok.extern.log4j.Log4j2;
import org.vstu.compprehension.models.businesslogic.domains.terms.methods.RegexTermDetectionMethod;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Log4j2
public class DomainTermAnnotationProcessor {
    private DomainTermDictionary dictionary;
    private Language language;

    public DomainTermAnnotationProcessor(DomainTermDictionary dictionary, Language language) {
        this.dictionary = dictionary;
        for (DomainTermElement term : dictionary) {
            if (term.isMalformed()) {
                log.warn("Term `%s` in loaded dictionary is malformed and won't be used in detection",
                        term.getPattern(language));
            } else if (term.isMalformed(language)) {
                log.warn("Term `%s` in loaded dictionary doesn't have support of %s language",
                        term.getPattern(language), language.toLocaleString());
            }
        }
        this.language = language;
    }

    public List<DomainTermAnnotation> annotate(String s) {
        List<DomainTermElement> originalTerms = dictionary.getTerms();
        List<DomainTermElement> sortedTerms = originalTerms.stream()
                .filter(i -> !i.isMalformed())
                .sorted(Comparator
                        // 1. RegexTermDetectionMethod выше остальных
                        .comparing((DomainTermElement e) -> !(dictionary.getDetectionMethod(e) instanceof RegexTermDetectionMethod))
                        // 2. По убыванию длины паттерна
                        .thenComparing(e -> -e.getPattern(language).length())
                        // 3. По изначальному порядку (индекс в оригинальном списке)
                        .thenComparing(originalTerms::indexOf)
                )
                .toList();
        List<DomainTermAnnotation> annotations = new ArrayList<>();

        for (DomainTermElement term : sortedTerms) {
            DomainTermDetectionMethod detection = dictionary.getDetectionMethod(term);
            var result = detection.findTerms(s, dictionary, term, language);
            for (DomainTermAnnotation annotation : result) {
                // Проверим отсутствие пересечений с ранее добавленными аннотациями
                boolean allow = true;
                for (DomainTermAnnotation added : annotations) {
                    if ((annotation.pos() >= added.pos() && annotation.pos() < added.pos() + added.length())
                            || (annotation.pos() + annotation.length() >= added.pos() &&
                            annotation.pos() + annotation.length() < added.pos() + added.length())
                    ) {
                        allow = false;
                        break;
                    }
                }
                if (allow) {
                    annotations.add(annotation);
                }
            }
        }
        return annotations;
    }

    public String apply(String s, DomainTermAnnotationVisualizer visualizer) {
        var annotations = annotate(s);

        // Сортируем аннотации по убыванию позиции — чтобы не нарушать позиции при замене
        annotations.sort(Comparator.comparingInt(DomainTermAnnotation::pos).reversed());

        StringBuilder result = new StringBuilder(s);

        for (DomainTermAnnotation annotation : annotations) {
            int start = annotation.pos();
            int end = start + annotation.length();

            String original = s.substring(start, end);
            String replacement = visualizer.apply(original, annotation, language);

            result.replace(start, end, replacement);
        }

        return result.toString();
    }
}
