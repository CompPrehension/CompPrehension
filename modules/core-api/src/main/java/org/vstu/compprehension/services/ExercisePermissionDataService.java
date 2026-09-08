package org.vstu.compprehension.services;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.permission.ExerciseCardPermissionsData;
import org.vstu.compprehension.data.permission.ExerciseListPermissionsData;
import org.vstu.compprehension.data.permission.UserPermissionsData;

public interface ExercisePermissionDataService {
    void ensureCanViewExercise(long userId, long exerciseId);

    ExerciseCardPermissionsData ofExercise(long userId, ExerciseData exercise, @Nullable Long courseId);

    ExerciseListPermissionsData ofExerciseList(long userId, @Nullable Long courseId);

    UserPermissionsData ofUser(long userId);
}
