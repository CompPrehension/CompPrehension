package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.exercise.ExerciseStageData;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Настройки упражнения, по которым идёт попытка: какими они были, когда попытка началась. */
public record AttemptExerciseData(
        @NotNull String domainId,
        @NotNull List<ExerciseStageData> stages,
        @NotNull List<String> tags,
        @Nullable Map<String, Object> answerReaction) implements Serializable {
}
