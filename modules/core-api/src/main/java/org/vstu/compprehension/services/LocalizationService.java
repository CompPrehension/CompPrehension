package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.Language;

import java.util.Locale;

public interface LocalizationService {
    @NotNull String getMessage(@NotNull String messageId, @NotNull Locale locale);
    @NotNull String getMessage(@NotNull String messageId, @NotNull Language language);
}
