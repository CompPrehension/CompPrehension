package org.vstu.compprehension.services;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.frontend.dto.ExerciseCardPermissionsDto;
import org.vstu.compprehension.frontend.dto.ExerciseListPermissionsDto;
import org.vstu.compprehension.frontend.dto.UserPermissionsDto;

public interface ExercisePermissionDataService {
    void ensureCanViewExercise(long userId, long exerciseId);

    ExerciseCardPermissionsDto ofExercise(long userId, ExerciseData exercise, @Nullable Long courseId);

    ExerciseListPermissionsDto ofExerciseList(long userId, @Nullable Long courseId);

    UserPermissionsDto ofUser(long userId);
}
