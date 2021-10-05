package org.vstu.compprehension.models.entities.EnumData;

import lombok.extern.log4j.Log4j2;
import org.vstu.compprehension.utils.StringHelper;

@Log4j2
public enum Language {
    RUSSIAN,
    ENGLISH;

    Language() {
    }

    public static Language fromString(String input) {
        if (StringHelper.startsWithIgnoreCase(input, "ru")) {
            return Language.RUSSIAN;
        }
        if (StringHelper.startsWithIgnoreCase(input, "en")) {
            return Language.ENGLISH;
        }

        log.warn("Couldn't recognize language '{}'. Using default instead (ENGLISH)", input);
        return Language.ENGLISH;
    }
}
