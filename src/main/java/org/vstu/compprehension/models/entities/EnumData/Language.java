package org.vstu.compprehension.models.entities.EnumData;

import lombok.extern.log4j.Log4j2;
import org.vstu.compprehension.utils.StringHelper;

import java.util.Locale;

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

    public static Locale getLocale(Language language) {
        Locale locale;
        if (language == Language.ENGLISH) {
            locale = Locale.ENGLISH;
        } else if (language == Language.RUSSIAN) {
            locale = new Locale("ru", "RU");
        } else if (language == Language.POLISH) {
            locale = new Locale("pl", "PL");
        } else {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    public String toLocaleString() {
        return getLocale(this).getLanguage().toUpperCase();
    }

    public Locale toLocale() {
        return getLocale(this);
    }
}
