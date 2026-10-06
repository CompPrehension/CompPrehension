package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.data.question.Assumption;
import org.vstu.compprehension.data.question.ViolationData;

import java.util.List;

/**
 * Рассуждение, которым студент мог прийти к ответу.
 *
 * @param id          идентификатор рассуждения в вердикте: по нему на рассуждение ссылаются уточнение и выбор студента
 * @param assumptions допущения хода мысли, от внешнего к вложенному
 * @param reason      как спросить студента, рассуждал ли он так; без неё рассуждение не предлагается на выбор
 */
public record Reasoning(int id,
                        @NotNull List<Assumption> assumptions,
                        @Nullable String reason,
                        @NotNull Explanation explanation,
                        @NotNull List<ViolationData> violations,
                        @NotNull List<String> appliedLaws) {

    public Reasoning {
        if (assumptions.isEmpty()) {
            throw new IllegalArgumentException("A reasoning consists of assumptions");
        }
        assumptions = List.copyOf(assumptions);
        violations = List.copyOf(violations);
        appliedLaws = List.copyOf(appliedLaws);
    }

    public boolean isCorrect() {
        return assumptions.stream().allMatch(Assumption::isCorrect);
    }

    /** Число ошибочных допущений: чем их меньше, тем вероятнее, что студент рассуждал так. */
    public long countErrors() {
        return assumptions.stream().filter(assumption -> !assumption.isCorrect()).count();
    }
}
