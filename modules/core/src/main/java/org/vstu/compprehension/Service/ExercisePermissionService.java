package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.ExerciseCardPermissionsDto;
import org.vstu.compprehension.dto.ExerciseListPermissionsDto;
import org.vstu.compprehension.dto.UserPermissionsDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

@Service
@RequiredArgsConstructor
public class ExercisePermissionService {
    private final AuthService authService;
    private final AuthScopeFactory authScopes;

    public ExerciseCardPermissionsDto ofExercise(long userId, ExerciseEntity exercise, @Nullable Long courseId) {
        var scoped = authService.getPermissions(userId, authScopes.courseOrGlobal(courseId));
        // Копирование в глобальный пул авторизуется в GLOBAL-области, а не в области страницы.
        var global = courseId == null ? scoped : authService.getPermissions(userId, authScopes.global());
        boolean inherited = ExerciseService.isInheritedInCourse(exercise, courseId);

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
