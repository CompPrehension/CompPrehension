package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.Capability;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.permission.ExerciseCardPermissionsData;
import org.vstu.compprehension.data.permission.ExerciseListPermissionsData;

import java.util.Set;

public interface AuthFrontendService {
    void ensureAuthorized(long userId, Permission permission, AuthScope scope);

    void ensureAuthorized(long userId, Capability capability);

    AuthScope getGlobalScope();

    AuthScope getCourseScope(long courseId);

    AuthScope getExerciseScope(long exerciseId, @Nullable Long courseId);

    void ensureCanViewExercise(long userId, long exerciseId);

    void ensureCanReadAttempt(long userId, long attemptId);

    void ensureCanWriteAttempt(long userId, long attemptId);

    void ensureCanReadQuestion(long userId, long questionId);

    void ensureCanWriteQuestion(long userId, long questionId);

    @NotNull ExerciseCardPermissionsData getExerciseCardPermissions(long userId, @NotNull ExerciseData exercise, @Nullable Long courseId);

    @NotNull ExerciseListPermissionsData getExerciseListPermissions(long userId, @Nullable Long courseId);

    boolean canViewGlobalPool(long userId);

    boolean canRegisterLms(long userId);

    void ensureCanRegisterLms(long userId);

    boolean canViewAllCourses(long userId);

    @NotNull Set<Long> findVisibleCourseIds(long userId);
}
