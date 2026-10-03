package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.data.question.InteractionReasoningData;
import org.vstu.compprehension.data.question.ViolationData;

import java.util.List;

/**
 * Рассуждение, которым студент мог прийти к ответу.
 *
 * @param hypothesis  гипотеза о ходе мысли; null — домен ход мысли не установил и объясняет сам ответ
 * @param reason      как спросить студента, рассуждал ли он так; без неё рассуждение не предлагается на выбор
 */
public record Reasoning(@Nullable String hypothesis,
                        boolean isCorrect,
                        @Nullable String reason,
                        @NotNull Explanation explanation,
                        @NotNull List<ViolationData> violations,
                        @NotNull List<String> appliedLaws) {

    public Reasoning {
        violations = List.copyOf(violations);
        appliedLaws = List.copyOf(appliedLaws);
    }

    public @NotNull InteractionReasoningData toData() {
        return new InteractionReasoningData(hypothesis, isCorrect, reason, violations, appliedLaws);
    }
}
