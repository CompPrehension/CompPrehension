package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

/** Уточняющий вопрос, заданный студенту после ответа, и ответил ли он на него. */
public record InteractionClarificationData(@NotNull HypothesisClarificationData content, boolean isAnswered) {
}
