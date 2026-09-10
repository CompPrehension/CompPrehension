package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.DomainDto;
import org.vstu.compprehension.mappers.Mapping;

public interface DomainDtoMapper extends Mapping {

    @NotNull DomainDto map(@NotNull Domain domain, @NotNull Language language);
}
