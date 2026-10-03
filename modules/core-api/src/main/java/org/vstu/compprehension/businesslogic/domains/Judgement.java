package org.vstu.compprehension.businesslogic.domains;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Вердикт домена по ответу на шаге вопроса: рассуждения, которыми студент мог к нему прийти.
 *
 * @param inquiry есть, если домен различает рассуждения; тогда у каждого рассуждения есть гипотеза
 */
public record Judgement(@NotNull List<Reasoning> reasonings, int stepsLeft, @Nullable ReasoningInquiry inquiry) {

    public Judgement {
        if (reasonings.isEmpty()) {
            throw new IllegalArgumentException("An answer is judged by at least one reasoning");
        }
        if (inquiry == null && (reasonings.size() > 1 || reasonings.getFirst().hypothesis() != null)) {
            throw new IllegalArgumentException("Reasonings with hypotheses need an inquiry");
        }
        if (inquiry != null && reasonings.stream().anyMatch(reasoning -> reasoning.hypothesis() == null)) {
            throw new IllegalArgumentException("An inquiry is made only about reasonings with hypotheses");
        }
        reasonings = List.copyOf(reasonings);
    }

    /** Вердикт, в котором ход мысли студента не установлен. */
    public Judgement(@NotNull Reasoning reasoning, int stepsLeft) {
        this(List.of(reasoning), stepsLeft, null);
    }

    public boolean isAnswerCorrect() {
        return reasonings.stream().anyMatch(Reasoning::isCorrect);
    }
}
