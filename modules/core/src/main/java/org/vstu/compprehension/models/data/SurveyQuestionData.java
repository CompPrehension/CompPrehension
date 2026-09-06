package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;

/**
 * Вопрос опроса.
 *
 * @param policy  правило показа: произвольный json, который разбирает фронт
 * @param options варианты ответа: произвольный json, зависящий от {@code type}
 */
public record SurveyQuestionData(
        long id,
        @NotNull String type,
        @NotNull String text,
        boolean required,
        @NotNull Object policy,
        @NotNull Object options) {
}
