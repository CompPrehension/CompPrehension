package org.vstu.compprehension.data.survey;

import org.jetbrains.annotations.Nullable;

/**
 * Ответ пользователя на вопрос опроса.
 */
public record SurveyVoteData(long surveyQuestionId, long questionId, @Nullable String answer) {
}
