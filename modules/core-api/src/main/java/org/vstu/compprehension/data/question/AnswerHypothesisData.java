package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

/** Способ рассуждения студента, которым объясняется его ответ. */
public record AnswerHypothesisData(@NotNull String name, boolean isCorrect) {
}
