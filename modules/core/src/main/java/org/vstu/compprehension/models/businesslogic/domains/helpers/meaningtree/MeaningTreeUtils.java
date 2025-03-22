package org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree;

import org.vstu.meaningtree.MeaningTree;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;
import org.vstu.meaningtree.languages.LanguageTranslator;

import java.lang.reflect.InvocationTargetException;

public class MeaningTreeUtils {
    public static String viewExpression(MeaningTree mt, SupportedLanguage lang)  {
        try {
            LanguageTranslator translator = lang.createTranslator(new MeaningTreeDefaultExpressionConfig());
            return translator.getCode(mt);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new MeaningTreeException(e);
        }
    }
}
