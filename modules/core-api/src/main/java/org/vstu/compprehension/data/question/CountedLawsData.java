package org.vstu.compprehension.data.question;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Законы, которые засчитываются студенту по ответу: нарушенные и верно применённые. */
@Getter
public final class CountedLawsData {
    private final @NotNull List<ViolationData> violations;
    private final @NotNull List<String> appliedLaws;

    // Верный ответ засчитывается по верным рассуждениям: заблуждение, которое тоже к нему ведёт, его не портит,
    // даже если студент назвал его в уточнении. У неверного ответа засчитывается известное рассуждение —
    // единственное или выбранное студентом, а пока оно неизвестно — только законы, общие для всех рассуждений.
    public CountedLawsData(boolean isAnswerCorrect,
                           @NotNull List<InteractionReasoningData> reasonings,
                           @Nullable Integer chosenReasoning) {
        if (isAnswerCorrect) {
            var correct = reasonings.stream().filter(InteractionReasoningData::isCorrect).toList();
            this.violations = correct.stream().flatMap(reasoning -> reasoning.violations().stream()).distinct().toList();
            this.appliedLaws = correct.stream().flatMap(reasoning -> reasoning.appliedLaws().stream()).distinct().toList();
            return;
        }
        var known = reasonings.size() == 1 ? reasonings.getFirst()
                : chosenReasoning != null ? reasonings.get(chosenReasoning)
                : null;
        if (known != null) {
            this.violations = known.violations();
            this.appliedLaws = known.appliedLaws();
            return;
        }
        var commonViolated = intersect(reasonings, reasoning -> reasoning.violations().stream()
                .map(ViolationData::getLawName).toList());
        this.violations = reasonings.isEmpty() ? List.of() : reasonings.getFirst().violations().stream()
                .filter(violation -> commonViolated.contains(violation.getLawName()))
                .toList();
        var commonApplied = intersect(reasonings, InteractionReasoningData::appliedLaws);
        this.appliedLaws = reasonings.isEmpty() ? List.of() : reasonings.getFirst().appliedLaws().stream()
                .filter(commonApplied::contains)
                .toList();
    }

    private static @NotNull Set<String> intersect(@NotNull List<InteractionReasoningData> reasonings,
                                                  @NotNull Function<InteractionReasoningData, Collection<String>> laws) {
        return reasonings.stream()
                .map(reasoning -> (Set<String>) Set.copyOf(laws.apply(reasoning)))
                .reduce((left, right) -> left.stream().filter(right::contains).collect(Collectors.toSet()))
                .orElse(Set.of());
    }
}
