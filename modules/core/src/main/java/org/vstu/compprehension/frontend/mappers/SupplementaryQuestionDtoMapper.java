package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.SupplementaryResponse;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.mappers.Mapping;

public interface SupplementaryQuestionDtoMapper extends Mapping {

    @NotNull SupplementaryQuestionDto map(@NotNull SupplementaryResponse response,
                                          @NotNull Language language);
}
