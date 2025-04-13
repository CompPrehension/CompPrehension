package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.languages.LanguageTranslator;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class MeaningTreeUtils {
    public static String viewExpression(MeaningTree mt, SupportedLanguage lang)  {
        try {
            LanguageTranslator translator = lang.createTranslator(new MeaningTreeDefaultExpressionConfig());
            return translator.getCode(mt);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException(e);
        }
    }

    // Определить по тегам язык программирования
    public static SupportedLanguage detectLanguageFromTags(Collection<String> tags) {
        // Считаем, что в тегах может быть указан только один язык
        for (String tag : tags) {
            SupportedLanguage result = SupportedLanguage.fromString(tag);
            if (result != null) {
                return result;
            }
        }
        return SupportedLanguage.CPP;
    }

    // Определить по тегам язык программирования
    public static SupportedLanguage detectLanguageFromTags(long tags, ProgrammingLanguageExpressionDTDomain domain) {
        // Считаем, что в тегах может быть указан только один язык
        for (Tag tag : domain.tagsFromBitmask(tags)) {
            SupportedLanguage result = SupportedLanguage.fromString(tag.getName());
            if (result != null) {
                return result;
            }
        }
        return SupportedLanguage.CPP;
    }
}
