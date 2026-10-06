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
 * @param hypotheses допущения хода мысли, от внешнего к вложенному; пусто — домен ход мысли не установил
 *                   и объясняет сам ответ
 * @param reason     как спросить студента, рассуждал ли он так; без неё рассуждение не предлагается на выбор
 */
public record Reasoning(@NotNull List<String> hypotheses,
                        boolean isCorrect,
                        @Nullable String reason,
                        @NotNull Explanation explanation,
                        @NotNull List<ViolationData> violations,
                        @NotNull List<String> appliedLaws) {

    public Reasoning {
        hypotheses = List.copyOf(hypotheses);
        violations = List.copyOf(violations);
        appliedLaws = List.copyOf(appliedLaws);
    }

    public @NotNull InteractionReasoningData toData() {
        return new InteractionReasoningData(hypotheses, isCorrect, reason, violations, appliedLaws);
    }
}
