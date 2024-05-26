package org.vstu.compprehension.models.businesslogic.auth;

import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;

import java.util.List;

public class AuthObjects {

    public static class Permissions {

        // EducationResource
        public static final SystemPermission ViewEducationResource = new SystemPermission(
                "ViewEducationResource",
                "Просмотр обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission CreateEducationResource = new SystemPermission(
                "CreateEducationResource",
                "Создание обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL));

        public static final SystemPermission EditEducationResource = new SystemPermission(
                "EditEducationResource",
                "Редактирование обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL));

        public static final SystemPermission DeleteEducationResource = new SystemPermission(
                "DeleteEducationResource",
                "Удаление обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL));

        // Courses
        public static final SystemPermission ViewCourse = new SystemPermission(
                "ViewCourse",
                "Просмотр курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission CreateCourse = new SystemPermission(
                "CreateCourse",
                "Создание курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE));

        public static final SystemPermission EditCourse = new SystemPermission(
                "EditCourse",
                "Редактирование курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission DeleteCourse = new SystemPermission(
                "DeleteCourse",
                "Удаление курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        // Exercises
        public static final SystemPermission ViewExercise = new SystemPermission(
                "ViewExercise",
                "Просмотр упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission CreateExercise = new SystemPermission(
                "CreateExercise",
                "Создание упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission EditExercise = new SystemPermission(
                "EditExercise",
                "Редактирование упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission DeleteExercise = new SystemPermission(
                "DeleteExercise",
                "Удаление упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        public static final SystemPermission SolveExercise = new SystemPermission(
                "SolveExercise",
                "Выполнение упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));
    }

    public static class Roles {

        public static final SystemRole GlobalAdmin = new SystemRole("GlobalAdmin", "Глобальный администратор",
                new SystemPermission[]{
                        Permissions.ViewEducationResource,
                        Permissions.CreateEducationResource,
                        Permissions.EditEducationResource,
                        Permissions.DeleteEducationResource,
                        Permissions.ViewCourse,
                        Permissions.EditCourse,
                        Permissions.CreateCourse,
                        Permissions.DeleteCourse,
                        Permissions.ViewExercise,
                        Permissions.EditExercise,
                        Permissions.CreateExercise,
                        Permissions.DeleteExercise,
                        Permissions.SolveExercise
        });

        public static final SystemRole EducationResourceAdmin = new SystemRole("EducationResourceAdmin", "Администратор обучающего ресурса",
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.CreateCourse,
                        Permissions.EditCourse,
                        Permissions.DeleteCourse,
                        Permissions.ViewExercise,
                        Permissions.EditExercise,
                        Permissions.CreateExercise,
                        Permissions.DeleteExercise,
                        Permissions.SolveExercise
        });

        public static final SystemRole Teacher = new SystemRole("Teacher", "Учитель",
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.CreateCourse,
                        Permissions.EditCourse,
                        Permissions.DeleteCourse,
                        Permissions.ViewExercise,
                        Permissions.CreateExercise,
                        Permissions.EditExercise,
                        Permissions.DeleteExercise,
                        Permissions.SolveExercise
        });

        public static final SystemRole Assistant = new SystemRole("Assistant", "Ассистент",
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.ViewExercise,
                        Permissions.CreateExercise,
                        Permissions.EditExercise,
                        Permissions.DeleteExercise,
                        Permissions.SolveExercise
        });

        public static final SystemRole Student = new SystemRole("Student", "Студент",
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.ViewExercise,
                        Permissions.SolveExercise
        });

        public static final SystemRole Guest = new SystemRole("Guest", "Гость",
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.ViewExercise
                });
    }
}