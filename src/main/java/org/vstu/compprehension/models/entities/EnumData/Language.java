package org.vstu.compprehension.models.entities.EnumData;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.utils.StringHelper;

import java.util.Locale;
import java.util.Objects;

@Log4j2
public enum Language {
    RUSSIAN,
    ENGLISH,
    POLISH;

    Language() {
    }

    public static Language fromString(String input) {
        if (StringHelper.startsWithIgnoreCase(input, "ru")) {
            return Language.RUSSIAN;
        }
        if (StringHelper.startsWithIgnoreCase(input, "en")) {
            return Language.ENGLISH;
        }
        if (StringHelper.startsWithIgnoreCase(input, "pl")) {
            return Language.POLISH;
        }

        log.warn("Couldn't recognize language '{}'. Using default instead (ENGLISH)", input);
        return Language.ENGLISH;
    }

    public static @NotNull Locale getLocale(@Nullable Language language) {
        if (language == null) {
            log.warn("Couldn't map language '{}' to locale. Using default instead (ENGLISH)", language);
            return Locale.ENGLISH;
        }

        switch (language) {
            case ENGLISH:
                return Locale.ENGLISH;
            case RUSSIAN:
                return new Locale("ru", "RU");
            case POLISH:
                return new Locale("pl", "PL");
            default:
                log.warn("Couldn't map language '{}' to locale. Using default instead (ENGLISH)", language);
                return Locale.ENGLISH;
        }
    }

    public String toLocaleString() {
        return getLocale(this).getLanguage().toUpperCase();
    }

    public Locale toLocale() {
        return getLocale(this);
    }
}
