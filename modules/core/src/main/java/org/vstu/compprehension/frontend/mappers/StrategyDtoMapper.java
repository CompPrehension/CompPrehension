package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.StrategyDto;
import org.vstu.compprehension.mappers.Mapping;

public interface StrategyDtoMapper extends Mapping {

    @NotNull StrategyDto map(@NotNull AbstractStrategy strategy, @NotNull Language language);
}
