package org.vstu.compprehension.models.businesslogic.backend.facts;

import its.model.definition.Domain;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO Class Description
 *
 * @author Marat Gumerov
 * @since 28.01.2024
 */
@AllArgsConstructor
@Getter
public class DTDomainDescriptionFact extends Fact {
    private final Domain domain;
}
