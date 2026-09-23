package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemCapability;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.Capability;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.businesslogic.auth.PermissionScopeKind;
import org.vstu.compprehension.data.exercise.ExerciseData;
import org.vstu.compprehension.data.exerciseattempt.AttemptOwnerData;
import org.vstu.compprehension.data.permission.ExerciseCardPermissionsData;
import org.vstu.compprehension.data.permission.ExerciseListPermissionsData;
import org.vstu.compprehension.services.AuthScopeFactory;
import org.vstu.compprehension.services.AuthService;
import org.vstu.compprehension.services.CourseDataService;
import org.vstu.compprehension.services.ExerciseAttemptDataService;
import org.vstu.compprehension.services.ExerciseDataService;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthFrontendServiceImpl implements AuthFrontendService {
    private final AuthService authService;
    private final AuthScopeFactory authScopeFactory;
    private final ExerciseDataService exerciseService;
    private final CourseDataService courseService;
    private final ExerciseAttemptDataService exerciseAttemptService;

    @Override
    public void ensureAuthorized(long userId, Permission permission, AuthScope scope) {
        authService.ensureAuthorized(userId, permission, scope);
    }

    @Override
    public void ensureAuthorized(long userId, Capability capability) {
        authService.ensureAuthorized(userId, capability);
    }

    @Override
    public AuthScope getGlobalScope() {
        return authScopeFactory.global();
    }

    @Override
    public AuthScope getCourseScope(long courseId) {
        return authScopeFactory.course(courseId);
    }

    @Override
    public AuthScope getExerciseScope(long exerciseId, @Nullable Long courseId) {
        return authScopeFactory.exercise(exerciseId, courseId);
    }

    @Override
    public void ensureCanViewExercise(long userId, long exerciseId) {
        if (exerciseService.isExercisePublic(exerciseId) && authService.isAuthorized(userId, SystemCapability.VIEW_GLOBAL_POOL)) {
            return;
        }
        var courseIds = courseService.findCourseIdsByExerciseId(exerciseId);
        if (!authService.isAuthorized(userId, SystemPermission.VIEW_EXERCISE_CARD, authScopeFactory.anyOfCourses(courseIds))) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to read exercise %s", userId, exerciseId));
        }
    }

    @Override
    public void ensureCanReadAttempt(long userId, long attemptId) {
        ensureAttemptOwnerOrObserver(userId, findOwnerOfAttempt(attemptId), attemptId);
    }

    @Override
    public void ensureCanWriteAttempt(long userId, long attemptId) {
        ensureAttemptOwner(userId, findOwnerOfAttempt(attemptId), attemptId);
    }

    @Override
    public void ensureCanReadQuestion(long userId, long questionId) {
        exerciseAttemptService.findOwnerByQuestionId(questionId).ifPresentOrElse(
                owner -> ensureAttemptOwnerOrObserver(userId, owner, questionId),
                () -> ensureCanAccessAttemptlessQuestion(userId, questionId));
    }

    @Override
    public void ensureCanWriteQuestion(long userId, long questionId) {
        exerciseAttemptService.findOwnerByQuestionId(questionId).ifPresentOrElse(
                owner -> ensureAttemptOwner(userId, owner, questionId),
                () -> ensureCanAccessAttemptlessQuestion(userId, questionId));
    }

    @Override
    public @NotNull ExerciseCardPermissionsData getExerciseCardPermissions(long userId, @NotNull ExerciseData exercise, @Nullable Long courseId) {
        var global = authService.getPermissions(userId, authScopeFactory.global());
        var scoped = courseId == null ? global : authService.getPermissions(userId, authScopeFactory.course(courseId));
        boolean inherited = exerciseService.isInheritedInCourse(exercise, courseId);

        return new ExerciseCardPermissionsData(
                !inherited && scoped.contains(SystemPermission.EDIT_EXERCISE),
                !inherited && scoped.contains(SystemPermission.DELETE_EXERCISE),
                inherited && scoped.contains(SystemPermission.CREATE_EXERCISE),
                !exercise.isPublic() && global.contains(SystemPermission.COPY_EXERCISE_TO_GLOBAL_POOL),
                inherited && scoped.contains(SystemPermission.LINK_POOL_EXERCISE_TO_COURSE)
        );
    }

    @Override
    public @NotNull ExerciseListPermissionsData getExerciseListPermissions(long userId, @Nullable Long courseId) {
        var scoped = authService.getPermissions(userId,
                courseId == null ? authScopeFactory.global() : authScopeFactory.course(courseId));
        boolean inCourse = courseId != null;
        return new ExerciseListPermissionsData(
                scoped.contains(SystemPermission.CREATE_EXERCISE),
                inCourse && scoped.contains(SystemPermission.LINK_POOL_EXERCISE_TO_COURSE),
                inCourse && scoped.contains(SystemPermission.CREATE_EXERCISE)
        );
    }

    @Override
    public boolean canViewGlobalPool(long userId) {
        return authService.isAuthorized(userId, SystemCapability.VIEW_GLOBAL_POOL);
    }

    @Override
    public boolean canViewAllCourses(long userId) {
        return authService.isAuthorized(userId, SystemPermission.VIEW_COURSE, authScopeFactory.root());
    }

    @Override
    public @NotNull Set<Long> findVisibleCourseIds(long userId) {
        var courseIds = new HashSet<>(authService.findScopeItemIdsWithPermission(
                userId, SystemPermission.VIEW_COURSE, PermissionScopeKind.COURSE));
        var educationResourceIds = authService.findScopeItemIdsWithPermission(
                userId, SystemPermission.VIEW_COURSE, PermissionScopeKind.EDUCATION_RESOURCE);
        courseIds.addAll(courseService.findCourseIdsByEducationResourceIds(educationResourceIds));
        return courseIds;
    }

    private @NotNull AttemptOwnerData findOwnerOfAttempt(long attemptId) {
        return exerciseAttemptService.findOwnerByAttemptId(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("No attempt with id " + attemptId));
    }

    private void ensureAttemptOwnerOrObserver(long userId, @NotNull AttemptOwnerData owner, long targetId) {
        if (isOwner(userId, owner)) {
            ensureAttemptOwner(userId, owner, targetId);
            return;
        }
        if (!authService.isAuthorized(userId, SystemPermission.VIEW_OTHER_ATTEMPTS, scopeOf(owner))) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to read attempt data %s", userId, targetId));
        }
    }

    private void ensureAttemptOwner(long userId, @NotNull AttemptOwnerData owner, long targetId) {
        if (!isOwner(userId, owner)) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to write attempt data %s", userId, targetId));
        }
        authService.ensureAuthorized(userId, SystemPermission.SOLVE_EXERCISE, scopeOf(owner));
    }

    private @NotNull AuthScope scopeOf(@NotNull AttemptOwnerData owner) {
        return owner.courseId() == null ? authScopeFactory.global() : authScopeFactory.course(owner.courseId());
    }

    private static boolean isOwner(long userId, @NotNull AttemptOwnerData owner) {
        return owner.userId() == userId;
    }

    private void ensureCanAccessAttemptlessQuestion(long userId, long questionId) {
        if (!authService.isAuthorized(userId, SystemCapability.DEBUG_BANK_QUESTION)) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to access attemptless question %s", userId, questionId));
        }
    }
}
