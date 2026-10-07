package org.vstu.compprehension.data.question;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Знания домена, которые засчитываются студенту по ответу: нарушенные и верно применённые. */
@Getter
public final class CountedKnowledgeData {
    private final @NotNull List<ViolationData> violations;
    private final @NotNull List<String> appliedKnowledge;

    // Верный ответ засчитывается по верным рассуждениям: заблуждение, которое тоже к нему ведёт, его не портит,
    // даже если студент назвал его в уточнении. У неверного ответа засчитывается известное рассуждение —
    // единственное отобранное или выбранное студентом, а пока оно неизвестно — только знания, общие для отобранных.
    public CountedKnowledgeData(boolean isAnswerCorrect,
                           @NotNull List<InteractionReasoningData> reasonings,
                           @Nullable Integer chosenReasoning) {
        if (isAnswerCorrect) {
            var correct = reasonings.stream().filter(InteractionReasoningData::isCorrect).toList();
            this.violations = correct.stream().flatMap(reasoning -> reasoning.getViolations().stream()).distinct().toList();
            this.appliedKnowledge = correct.stream().flatMap(reasoning -> reasoning.getAppliedKnowledge().stream()).distinct().toList();
            return;
        }
        var probable = reasonings.stream().filter(InteractionReasoningData::isProbable).toList();
        var known = chosenReasoning != null
                ? reasonings.stream().filter(reasoning -> reasoning.getId() == chosenReasoning).findFirst().orElseThrow()
                : probable.size() == 1 ? probable.getFirst()
                : null;
        if (known != null) {
            this.violations = known.getViolations();
            this.appliedKnowledge = known.getAppliedKnowledge();
            return;
        }
        var commonViolated = intersect(probable, reasoning -> reasoning.getViolations().stream()
                .map(ViolationData::getKnowledgeName).toList());
        this.violations = probable.isEmpty() ? List.of() : probable.getFirst().getViolations().stream()
                .filter(violation -> commonViolated.contains(violation.getKnowledgeName()))
                .toList();
        var commonApplied = intersect(probable, InteractionReasoningData::getAppliedKnowledge);
        this.appliedKnowledge = probable.isEmpty() ? List.of() : probable.getFirst().getAppliedKnowledge().stream()
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
