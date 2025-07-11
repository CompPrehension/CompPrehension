package org.vstu.compprehension.models.businesslogic.domains.terms;

import org.vstu.compprehension.models.entities.EnumData.Language;

public record DomainTermAnnotation(DomainTermElement term, Language language,
                                   int pos, int length, String pattern) {
    public String explanation() {
        return term.getExplanation(language);
    }
}
