package org.vstu.compprehension.models.businesslogic.domains.terms;

import org.vstu.compprehension.models.entities.EnumData.Language;

public interface DomainTermDetectionMethod {
    DomainTermAnnotation[] findTerms(String source, DomainTermDictionary dictionary,
                                     DomainTermElement element, Language language);
}
