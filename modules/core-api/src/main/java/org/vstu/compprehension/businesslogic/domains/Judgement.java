package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.Explanation;
import org.vstu.compprehension.data.question.ViolationData;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Вердикт домена по ответу на шаге вопроса. */
public sealed interface Judgement {

    int stepsLeft();

    boolean isAnswerCorrect();

    /** Ход мысли студента не установлен: домен объясняет сам ответ. */
    record Verdict(boolean isAnswerCorrect,
                   @NotNull Explanation explanation,
                   @NotNull List<ViolationData> violations,
                   @NotNull List<String> appliedKnowledge,
                   int stepsLeft) implements Judgement {

        public Verdict {
            violations = List.copyOf(violations);
            appliedKnowledge = List.copyOf(appliedKnowledge);
        }
    }

    /**
     * Ответ объясняют рассуждения, которыми студент мог к нему прийти.
     *
     * @param inquiry как говорить со студентом об ответе, пока его рассуждение неизвестно
     */
    record Reasoned(@NotNull List<Reasoning> reasonings,
                    @NotNull ReasoningInquiry inquiry,
                    int stepsLeft) implements Judgement {

        public Reasoned {
            if (reasonings.isEmpty()) {
                throw new IllegalArgumentException("An answer is explained by at least one reasoning");
            }
            if (reasonings.stream().map(Reasoning::id).distinct().count() != reasonings.size()) {
                throw new IllegalArgumentException("Reasonings of a judgement have distinct ids");
            }
            reasonings = List.copyOf(reasonings);
        }

        @Override
        public boolean isAnswerCorrect() {
            return reasonings.stream().anyMatch(Reasoning::isCorrect);
        }

        public @NotNull Set<Integer> collectReasoningIds() {
            return reasonings.stream().map(Reasoning::id).collect(Collectors.toUnmodifiableSet());
        }

        public @NotNull Reasoning getReasoning(int id) {
            return reasonings.stream()
                    .filter(reasoning -> reasoning.id() == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No reasoning " + id + " in the judgement"));
        }
    }
}
