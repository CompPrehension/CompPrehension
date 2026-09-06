package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.Nullable;

/**
 * Пара вариантов, выбранная студентом, — ровно то, что пришло с фронта.
 * <p>
 * Варианты названы своими номерами внутри вопроса, а не идентификаторами строк: фронт
 * знает только их, и превращение номера в строку — работа слоя доступа к данным.
 *
 * @param createdByInteractionId взаимодействие, в котором этот ответ был дан впервые;
 *                               null — ответ дан сейчас
 */
public record SubmittedAnswerData(
        int leftAnswerId,
        int rightAnswerId,
        @Nullable Long createdByInteractionId) {
}
