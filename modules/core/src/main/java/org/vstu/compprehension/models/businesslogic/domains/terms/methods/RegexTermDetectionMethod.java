package org.vstu.compprehension.models.businesslogic.domains.terms.methods;

import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermAnnotation;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDetectionMethod;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermDictionary;
import org.vstu.compprehension.models.businesslogic.domains.terms.DomainTermElement;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTermDetectionMethod implements DomainTermDetectionMethod {

    public record MatchResult(String text, int pos, int length) {}

    public static List<MatchResult> regexSearch(String text, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(text);

        List<MatchResult> matches = new ArrayList<>();

        while (matcher.find()) {
            String matchText = matcher.group();
            int start = matcher.start();
            int length = matcher.end() - matcher.start();
            matches.add(new MatchResult(matchText, start, length));
        }

        return matches;
    }

    @Override
    public DomainTermAnnotation[] findTerms(String source, DomainTermDictionary dictionary,
                                            DomainTermElement element, Language language) {
        return regexSearch(source, element.getPattern(language))
                .stream()
                .map(match -> new DomainTermAnnotation(
                        element, language,
                        match.pos,
                        match.length
                ))
                .toArray(DomainTermAnnotation[]::new);
    }
}