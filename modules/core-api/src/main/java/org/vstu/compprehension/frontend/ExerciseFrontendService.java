package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseInfoDto;
import org.vstu.compprehension.frontend.dto.ExerciseListDto;

public interface ExerciseFrontendService {
    void ensureCanViewExercise(long userId, long exerciseId);

    @NotNull ExerciseInfoDto getExerciseShortInfo(long id, @Nullable Long courseId);

    void ensureExerciseExists(long exerciseId, @Nullable Long courseId);

    @NotNull ExerciseCardDto getExerciseCard(long exerciseId, @Nullable Long courseId, long userId);

    @NotNull ExerciseListDto listExercises(@Nullable Long courseId, long userId);

    void saveExerciseCard(@NotNull ExerciseCardDto card, @Nullable Long courseId);

    long createExerciseAndGetId(@NotNull String name, @NotNull String domainId, @NotNull String strategyId, @Nullable Long courseId);

    long cloneExerciseAndGetId(long sourceExerciseId, @Nullable Long targetCourseId);

    void deleteExercise(long exerciseId, @Nullable Long courseId);
}
