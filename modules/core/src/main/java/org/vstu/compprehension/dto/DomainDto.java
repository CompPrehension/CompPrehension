package org.vstu.compprehension.dto;

import lombok.Builder;
import lombok.Value;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.Law;

import java.util.List;

@Value @Builder
public class DomainDto {
    String id;
    String name;
    List<Law> laws;
    List<Concept> concepts;
}
