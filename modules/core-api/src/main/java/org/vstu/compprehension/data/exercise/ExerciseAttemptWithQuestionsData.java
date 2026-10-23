package org.vstu.compprehension.data.exercise;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;

import java.util.List;

public record ExerciseAttemptWithQuestionsData(
        long id,
        long userId,
        @NotNull AttemptExerciseData exercise,
        @NotNull List<AttemptQuestionData> questions) {
}
