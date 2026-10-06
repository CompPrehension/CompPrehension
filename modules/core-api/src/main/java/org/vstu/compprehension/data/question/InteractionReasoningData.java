package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * Рассуждение, которым студент мог прийти к ответу, в том виде, в каком оно хранится у взаимодействия.
 *
 * @param hypotheses допущения хода мысли, от внешнего к вложенному; пусто — домен ход мысли не установил
 *                   и объясняет сам ответ
 */
public record InteractionReasoningData(@NotNull List<String> hypotheses,
                                       boolean isCorrect,
                                       @Nullable String reason,
                                       @NotNull List<ViolationData> violations,
                                       @NotNull List<String> appliedLaws) implements Serializable {

    public InteractionReasoningData {
        hypotheses = List.copyOf(hypotheses);
        violations = List.copyOf(violations);
        appliedLaws = List.copyOf(appliedLaws);
    }
}
