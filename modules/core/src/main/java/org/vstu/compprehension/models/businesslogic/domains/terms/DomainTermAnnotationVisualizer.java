package org.vstu.compprehension.models.businesslogic.domains.terms;

import org.vstu.compprehension.models.entities.EnumData.Language;

public interface DomainTermAnnotationVisualizer {
    String apply(String original, DomainTermAnnotation anno, Language lang);
}
