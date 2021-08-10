package org.vstu.compprehension.utils;

import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.HashMap;

/**
 * A mapping from user Language (ex. English, Russian) to localized strings.
 */
public class LocalizationMap extends HashMap<Language, String> {
    public LocalizationMap() {
        super(Language.values().length);
    }
}
