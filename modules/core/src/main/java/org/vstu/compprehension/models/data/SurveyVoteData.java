package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.Nullable;

/**
 * Ответ пользователя на вопрос опроса, данный при решении конкретного вопроса упражнения.
 * <p>
 * Пользователь в записи не назван: голоса всегда читаются и пишутся для одного
 * пользователя, и он известен вызывающему.
 *
 * @param answer пустой ответ допускает сама схема: вопрос опроса может быть
 *               необязательным, и тогда пользователь оставляет его без ответа
 */
public record SurveyVoteData(long surveyQuestionId, long questionId, @Nullable String answer) {
}
