package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsSchema;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.StrategyDto;
import org.vstu.compprehension.frontend.dto.StrategySettingsDto;

import java.util.List;

@Component
class StrategyDtoMapperImpl implements StrategyDtoMapper {

    @Override
    public @NotNull StrategyDto map(@NotNull AbstractStrategy strategy, @NotNull Language language) {
        var settingsType = strategy.getSettingsType();
        return StrategyDto.builder()
                .id(strategy.getStrategyId())
                .displayName(strategy.getDisplayName(language))
                .description(strategy.getDescription(language))
                .options(strategy.getOptions())
                .settings(new StrategySettingsDto(
                        mapFields(settingsType.getSchema().fields(), language), settingsType.getDefaults()))
                .build();
    }

    private @NotNull List<StrategySettingsDto.Field> mapFields(@NotNull List<StrategySettingsSchema.Field> fields,
                                                              @NotNull Language language) {
        return fields.stream().map(field -> mapField(field, language)).toList();
    }

    private @NotNull StrategySettingsDto.Field mapField(@NotNull StrategySettingsSchema.Field field, @NotNull Language language) {
        var label = field.label().toText(language);
        return switch (field) {
            case StrategySettingsSchema.Flag flag -> new StrategySettingsDto.Flag(flag.name(), label);
            case StrategySettingsSchema.Numeric numeric ->
                    new StrategySettingsDto.Numeric(numeric.name(), label, numeric.min(), numeric.max());
            case StrategySettingsSchema.Choice choice -> new StrategySettingsDto.Choice(choice.name(), label,
                    choice.options().stream()
                            .map(option -> new StrategySettingsDto.Option(option.value(), option.label().toText(language)))
                            .toList());
            case StrategySettingsSchema.Group group ->
                    new StrategySettingsDto.Group(group.name(), label, mapFields(group.fields(), language));
        };
    }
}
