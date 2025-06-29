package org.vstu.compprehension.models.businesslogic.domains.terms;

public class DomainTermTooltipVisualizer implements DomainTermAnnotationVisualizer {
    @Override
    public String apply(String s, DomainTermAnnotation domainTermAnnotation) {
        return "<span data-explanation=\'%s\' data-term=\'%s\', class=\'domain-term\'>%s</span>"
                .formatted(domainTermAnnotation.explanation(), domainTermAnnotation.term().getPattern(), s);
    }
}
