package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Уточняющий вопрос, заданный студенту после ответа, и ответ на него.
 *
 * @param chosenReasoning номер выбранного рассуждения взаимодействия; null — не ответил или назвал другую причину
 */
public record InteractionClarificationData(@NotNull HypothesisClarificationData content,
                                           boolean isAnswered,
                                           @Nullable Integer chosenReasoning) {
}
