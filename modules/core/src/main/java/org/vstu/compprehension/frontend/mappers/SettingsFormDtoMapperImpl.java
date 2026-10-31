package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.settings.SettingsSchema;
import org.vstu.compprehension.businesslogic.settings.SettingsType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SettingsFormDto;

import java.util.List;

@Component
class SettingsFormDtoMapperImpl implements SettingsFormDtoMapper {

    @Override
    public @NotNull SettingsFormDto map(@NotNull SettingsType<?> settingsType, @NotNull Language language) {
        return new SettingsFormDto(mapFields(settingsType.getSchema().fields(), language), settingsType.getDefaults());
    }

    private @NotNull List<SettingsFormDto.Field> mapFields(@NotNull List<SettingsSchema.Field> fields,
                                                          @NotNull Language language) {
        return fields.stream().map(field -> mapField(field, language)).toList();
    }

    private @NotNull SettingsFormDto.Field mapField(@NotNull SettingsSchema.Field field, @NotNull Language language) {
        var label = field.label().toText(language);
        return switch (field) {
            case SettingsSchema.Flag flag -> new SettingsFormDto.Flag(flag.name(), label);
            case SettingsSchema.Numeric numeric ->
                    new SettingsFormDto.Numeric(numeric.name(), label, numeric.min(), numeric.max());
            case SettingsSchema.Choice choice -> new SettingsFormDto.Choice(choice.name(), label,
                    choice.options().stream()
                            .map(option -> new SettingsFormDto.Option(option.value(), option.label().toText(language)))
                            .toList());
            case SettingsSchema.Group group ->
                    new SettingsFormDto.Group(group.name(), label, mapFields(group.fields(), language));
        };
    }
}
