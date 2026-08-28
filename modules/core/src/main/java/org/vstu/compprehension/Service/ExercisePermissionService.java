package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.ExerciseCardPermissionsDto;
import org.vstu.compprehension.dto.ExerciseListPermissionsDto;
import org.vstu.compprehension.dto.UserPermissionsDto;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.Permission;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExercisePermissionService {
    private final AuthService authService;

    public ExerciseCardPermissionsDto ofExercise(long userId, ExerciseEntity exercise, @Nullable Long courseId) {
        var scoped = scopePermissions(userId, courseId);
        // Копирование в глобальный пул авторизуется в GLOBAL-области, а не в области страницы.
        var global = courseId == null ? scoped : authService.getGlobalPermissions(userId);
        boolean inherited = ExerciseService.isInheritedInCourse(exercise, courseId);

        return new ExerciseCardPermissionsDto(
                !inherited && scoped.contains(Permission.EDIT_EXERCISE),
                !inherited && scoped.contains(Permission.DELETE_EXERCISE),
                inherited && scoped.contains(Permission.CREATE_EXERCISE),
                !exercise.isPublic() && global.contains(Permission.CREATE_EXERCISE),
                inherited && scoped.contains(Permission.EDIT_COURSE)
        );
    }

    public ExerciseListPermissionsDto ofExerciseList(long userId, @Nullable Long courseId) {
        var scoped = scopePermissions(userId, courseId);
        boolean inCourse = courseId != null;
        return new ExerciseListPermissionsDto(
                scoped.contains(Permission.CREATE_EXERCISE),
                inCourse && scoped.contains(Permission.EDIT_COURSE),
                inCourse && scoped.contains(Permission.CREATE_EXERCISE)
        );
    }

    public UserPermissionsDto ofUser(long userId) {
        return new UserPermissionsDto(
                authService.getGlobalPermissions(userId).contains(Permission.VIEW_EXERCISE)
        );
    }

    private Set<Permission> scopePermissions(long userId, @Nullable Long courseId) {
        return courseId != null
                ? authService.getCoursePermissions(userId, courseId)
                : authService.getGlobalPermissions(userId);
    }
}
