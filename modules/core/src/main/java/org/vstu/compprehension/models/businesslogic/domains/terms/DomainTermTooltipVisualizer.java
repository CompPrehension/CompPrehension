package org.vstu.compprehension.models.businesslogic.domains.terms;

import org.vstu.compprehension.models.entities.EnumData.Language;

public class DomainTermTooltipVisualizer implements DomainTermAnnotationVisualizer {
    @Override
    public String apply(String s, DomainTermAnnotation domainTermAnnotation, Language lang) {
        return "<span data-explanation=\'%s\' data-term=\'%s\', class=\'domain-term\'>%s</span>"
                .formatted(domainTermAnnotation.explanation(), domainTermAnnotation.pattern(), s);
    }
}
