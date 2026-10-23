package org.vstu.compprehension.data.survey;

import org.jetbrains.annotations.NotNull;

/**
 * Вопрос опроса.
 */
public record SurveyQuestionData(
        long id,
        @NotNull String type,
        @NotNull String text,
        boolean required,
        @NotNull Object policy,
        @NotNull Object options) {
}
