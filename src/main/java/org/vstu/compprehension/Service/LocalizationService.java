package org.vstu.compprehension.Service;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.Locale;

@Service
@Log4j2
public class LocalizationService {
    @Autowired
    private MessageSource messageSource;

    public @NotNull String getMessage(@NotNull String messageId, @NotNull Locale locale) {
        try {
            return messageSource.getMessage(messageId, null, locale);
        } catch (Exception e) {
            log.warn("Couldn't resolve message '{}' for language '{}'. {}", messageId, locale.getLanguage(), e);
            return messageId;
        }
    }

    public @NotNull String getMessage(@NotNull String messageId, @NotNull Language language) {
        return getMessage(messageId, Language.getLocale(language));
    }
}
