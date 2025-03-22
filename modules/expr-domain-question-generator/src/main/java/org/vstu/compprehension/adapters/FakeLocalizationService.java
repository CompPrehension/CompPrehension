package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.Locale;

public class FakeLocalizationService implements LocalizationService {
    @NotNull
    @Override
    public String getMessage(@NotNull String messageId, @NotNull Locale locale) {
        return messageId;
    }

    @NotNull
    @Override
    public String getMessage(@NotNull String messageId, @NotNull Language language) {
        return messageId;
    }
}
