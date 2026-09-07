package org.vstu.compprehension.data.survey;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Опрос вместе со своими вопросами.
 * <p>
 * Вопросы всегда здесь: опрос без них не показать, а раньше их подъём зависел от того,
 * дожил ли вызов до закрытия транзакции — связь ленивая, и маппер обходил её уже за
 * границей сервиса.
 */
public record SurveyData(
        @NotNull String surveyId,
        @NotNull SurveyOptionsData options,
        @NotNull List<SurveyQuestionData> questions) {
}
