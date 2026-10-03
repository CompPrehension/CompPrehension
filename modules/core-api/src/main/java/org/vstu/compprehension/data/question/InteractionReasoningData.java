package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * Рассуждение, которым студент мог прийти к ответу, в том виде, в каком оно хранится у взаимодействия.
 *
 * @param hypothesis гипотеза о ходе мысли; null — домен ход мысли не установил и объясняет сам ответ
 */
public record InteractionReasoningData(@Nullable String hypothesis,
                                       boolean isCorrect,
                                       @Nullable String reason,
                                       @NotNull List<ViolationData> violations,
                                       @NotNull List<String> appliedLaws) implements Serializable {

    public InteractionReasoningData {
        violations = List.copyOf(violations);
        appliedLaws = List.copyOf(appliedLaws);
    }
}
