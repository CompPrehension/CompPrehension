package org.vstu.compprehension.frontend.dto.feedback;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Ответ студента на уточняющий вопрос: номер выбранного варианта; не указан, если причина другая. */
public record ClarificationAnswerDto(@NotNull Long questionId, @Nullable Integer option) {
}
