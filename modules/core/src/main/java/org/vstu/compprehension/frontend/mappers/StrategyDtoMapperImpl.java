package org.vstu.compprehension.frontend.mappers;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.StrategyDto;

@Component
@RequiredArgsConstructor
class StrategyDtoMapperImpl implements StrategyDtoMapper {
    private final SettingsFormDtoMapper settingsFormDtoMapper;

    @Override
    public @NotNull StrategyDto map(@NotNull AbstractStrategy strategy, @NotNull Language language) {
        return StrategyDto.builder()
                .id(strategy.getStrategyId())
                .displayName(strategy.getDisplayName(language))
                .description(strategy.getDescription(language))
                .options(strategy.getOptions())
                .settings(settingsFormDtoMapper.map(strategy.getSettingsType(), language))
                .build();
    }
}
