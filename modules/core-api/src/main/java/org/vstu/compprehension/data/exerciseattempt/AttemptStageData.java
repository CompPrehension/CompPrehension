package org.vstu.compprehension.data.exerciseattempt;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exercise.ExerciseAttemptWithQuestionsData;
import org.vstu.compprehension.data.exercise.ExerciseStageData;

import java.util.List;

/** Этап упражнения в попытке: его настройки и вопросы, заданные для него. */
@Getter
public final class AttemptStageData {
    private final @NotNull ExerciseAttemptWithQuestionsData attempt;
    private final int index;
    private final @NotNull ExerciseStageData stage;
    private final @NotNull List<AttemptQuestionData> questions;

    public AttemptStageData(@NotNull ExerciseAttemptWithQuestionsData attempt, int index) {
        this.attempt = attempt;
        this.index = index;
        this.stage = attempt.exercise().stages().get(index);
        this.questions = attempt.questions().stream()
                .filter(question -> question.stageIndex() != null && question.stageIndex() == index)
                .toList();
    }
}
