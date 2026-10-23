package org.vstu.compprehension.data.survey;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Опрос вместе со своими вопросами.
 */
public record SurveyData(
        @NotNull String surveyId,
        @NotNull SurveyOptionsData options,
        @NotNull List<SurveyQuestionData> questions) {
}
