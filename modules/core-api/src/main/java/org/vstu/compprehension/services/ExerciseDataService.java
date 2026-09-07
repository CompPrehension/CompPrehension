package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseDto;

import java.util.List;

public interface ExerciseDataService {
    boolean isExercisePublic(long exerciseId);

    @NotNull ExerciseData getExercise(long exerciseId);

    @NotNull ExerciseOptionsData getExerciseOptionsInContext(long exerciseId, @Nullable Long courseId);

    @NotNull ExerciseData getExerciseInContext(long exerciseId, @Nullable Long courseId);

    boolean isInheritedInCourse(@NotNull ExerciseData exercise, @Nullable Long courseId);

    void ensureNotInheritedInCourse(long exerciseId, @Nullable Long courseId);

    long createExerciseAndGetId(@NotNull String name, @NotNull String domainId, @NotNull String strategyId, @Nullable Long courseId);

    long cloneExerciseAndGetId(long sourceExerciseId, @Nullable Long targetCourseId);

    void deleteExercise(long exerciseId);

    List<ExerciseDto> getCourseExercises(long courseId);

    List<ExerciseDto> getPublicExercises();

    void saveExerciseCard(@NotNull ExerciseCardDto card);
}
