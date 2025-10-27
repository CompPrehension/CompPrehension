package org.vstu.compprehension.models.businesslogic.domains.helpers;

import io.brookite.termannotations.DomainTermAnnotation;
import io.brookite.termannotations.DomainTermAnnotationVisualizer;

import java.util.Locale;

public class DomainTermTooltipVisualizer implements DomainTermAnnotationVisualizer {
    @Override
    public String apply(String s, DomainTermAnnotation domainTermAnnotation, Locale lang) {
        return "<span data-explanation=\'%s\' data-term=\'%s\', class=\'domain-term\'>%s</span>"
                .formatted(domainTermAnnotation.explanation(), domainTermAnnotation.pattern(), s);
    }
}
