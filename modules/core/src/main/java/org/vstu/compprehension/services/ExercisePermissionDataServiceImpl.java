package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.frontend.dto.ExerciseCardPermissionsDto;
import org.vstu.compprehension.frontend.dto.ExerciseListPermissionsDto;
import org.vstu.compprehension.frontend.dto.UserPermissionsDto;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.data.exercise.ExerciseData;

@Service
@RequiredArgsConstructor
class ExercisePermissionDataServiceImpl implements ExercisePermissionDataService {
    private final AuthService authService;
    private final AuthScopeFactoryImpl authScopes;
    private final ExerciseDataService exerciseService;
    private final CourseDataService courseService;

    public void ensureCanViewExercise(long userId, long exerciseId) {
        if (exerciseService.isExercisePublic(exerciseId) && authService.isAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.global())) {
            return;
        }
        var courseIds = courseService.findCourseIdsByExerciseId(exerciseId);
        if (!authService.isAuthorized(userId, SystemPermission.VIEW_EXERCISE, authScopes.anyOfCourses(courseIds))) {
            throw new SecurityException(String.format(
                    "User %s is not allowed to read exercise %s", userId, exerciseId));
        }
    }

    public ExerciseCardPermissionsDto ofExercise(long userId, ExerciseData exercise, @Nullable Long courseId) {
        var scoped = authService.getPermissions(userId, authScopes.courseOrGlobal(courseId));
        // Копирование в глобальный пул авторизуется в GLOBAL-области, а не в области страницы.
        var global = courseId == null ? scoped : authService.getPermissions(userId, authScopes.global());
        boolean inherited = exerciseService.isInheritedInCourse(exercise, courseId);

        return new ExerciseCardPermissionsDto(
                !inherited && scoped.contains(SystemPermission.EDIT_EXERCISE),
                !inherited && scoped.contains(SystemPermission.DELETE_EXERCISE),
                inherited && scoped.contains(SystemPermission.CREATE_EXERCISE),
                !exercise.isPublic() && global.contains(SystemPermission.CREATE_EXERCISE),
                inherited && scoped.contains(SystemPermission.MANAGE_COURSE_CONTENT)
        );
    }

    public ExerciseListPermissionsDto ofExerciseList(long userId, @Nullable Long courseId) {
        var scoped = authService.getPermissions(userId, authScopes.courseOrGlobal(courseId));
        boolean inCourse = courseId != null;
        return new ExerciseListPermissionsDto(
                scoped.contains(SystemPermission.CREATE_EXERCISE),
                inCourse && scoped.contains(SystemPermission.MANAGE_COURSE_CONTENT),
                inCourse && scoped.contains(SystemPermission.CREATE_EXERCISE)
        );
    }

    public UserPermissionsDto ofUser(long userId) {
        return new UserPermissionsDto(
                authService.getPermissions(userId, authScopes.global()).contains(SystemPermission.VIEW_EXERCISE)
        );
    }
}
