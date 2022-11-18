package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;

import java.util.List;
import java.util.Map;

@Value @Builder
public class DomainDto {
    String id;
    String name;
    List<Law> laws;
    List<Concept> concepts;
    Map<Concept, List<Concept>> concepts2;
}
