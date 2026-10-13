package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.settings.SettingsType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.SettingsFormDto;
import org.vstu.compprehension.mappers.Mapping;

public interface SettingsFormDtoMapper extends Mapping {

    @NotNull SettingsFormDto map(@NotNull SettingsType<?> settingsType, @NotNull Language language);
}
