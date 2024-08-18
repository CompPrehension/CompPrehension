package org.vstu.compprehension.models.businesslogic.auth;

import java.util.List;

/** Системные объекты авторизации */
public class AuthObjects {
    /** Атомарное системное право (permission)
     * @param Name Идентификатор права
     * @param DisplayName Отображаемое имя
     * @param AllowedScopes Разрешенные области действия права
     * */
    public record SystemPermission(String Name, String DisplayName, List<PermissionScopeKind> AllowedScopes) { }

    /** Системная роль
     * @param Name Идентификатор роли
     * @param DisplayName Отображаемое имя
     * @param Scope Область действия
     * */
    public record SystemRole(String Name, String DisplayName, PermissionScopeKind Scope, SystemPermission[] Permissions) {}

    /** Системные права */
    public static class Permissions {
        /** Право на создание образовательного ресурса */
        public static final SystemPermission CreateEducationResource = new SystemPermission(
                "CreateEducationResource",
                "Создание обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL));

        /** Право на просмотр образовательного ресурса */
        public static final SystemPermission ViewEducationResource = new SystemPermission(
                "ViewEducationResource",
                "Просмотр обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE));

        /** Право на редактирование образовательного ресурса */
        public static final SystemPermission EditEducationResource = new SystemPermission(
                "EditEducationResource",
                "Редактирование обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE));

        /** Право на удаление образовательного ресурса */
        public static final SystemPermission DeleteEducationResource = new SystemPermission(
                "DeleteEducationResource",
                "Удаление обучающих ресурсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE));

        /** Право на создание курса */
        public static final SystemPermission CreateCourse = new SystemPermission(
                "CreateCourse",
                "Создание курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE));

        /** Право на просмотр курса */
        public static final SystemPermission ViewCourse = new SystemPermission(
                "ViewCourse",
                "Просмотр курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на редактирование курса */
        public static final SystemPermission EditCourse = new SystemPermission(
                "EditCourse",
                "Редактирование курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на удаление курса */
        public static final SystemPermission DeleteCourse = new SystemPermission(
                "DeleteCourse",
                "Удаление курсов",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на просмотр настроек упражнений */
        public static final SystemPermission ViewExercise = new SystemPermission(
                "ViewExercise",
                "Просмотр упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на создание упражнений */
        public static final SystemPermission CreateExercise = new SystemPermission(
                "CreateExercise",
                "Создание упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на редактирование упражнений */
        public static final SystemPermission EditExercise = new SystemPermission(
                "EditExercise",
                "Редактирование упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на удаление упражнений */
        public static final SystemPermission DeleteExercise = new SystemPermission(
                "DeleteExercise",
                "Удаление упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));

        /** Право на выполнение упражнений */
        public static final SystemPermission RunExercise = new SystemPermission(
                "RunExercise",
                "Выполнение упражнений",
                List.of(PermissionScopeKind.GLOBAL, PermissionScopeKind.EDUCATION_RESOURCE, PermissionScopeKind.COURSE));
    }

    /** Системные роли */
    public static class Roles {

        public static final SystemRole GlobalAdmin = new SystemRole(
                "GlobalAdmin",
                "Глобальный администратор",
                PermissionScopeKind.GLOBAL,
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
                        Permissions.RunExercise
                });

        public static final SystemRole EducationResourceAdmin = new SystemRole(
                "EducationResourceAdmin",
                "Администратор обучающего ресурса",
                PermissionScopeKind.EDUCATION_RESOURCE,
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.CreateCourse,
                        Permissions.EditCourse,
                        Permissions.DeleteCourse,
                        Permissions.ViewExercise,
                        Permissions.EditExercise,
                        Permissions.CreateExercise,
                        Permissions.DeleteExercise,
                        Permissions.RunExercise
                });

        public static final SystemRole Teacher = new SystemRole(
                "Teacher",
                "Учитель",
                PermissionScopeKind.COURSE,
                new SystemPermission[]{
                        Permissions.ViewCourse,
                        Permissions.EditCourse,
                        Permissions.DeleteCourse,
                        Permissions.ViewExercise,
                        Permissions.CreateExercise,
                        Permissions.EditExercise,
                        Permissions.DeleteExercise,
                        Permissions.RunExercise
                });

        public static final SystemRole Assistant = new SystemRole(
                "Assistant",
                "Ассистент",
                PermissionScopeKind.COURSE,
                new SystemPermission[] {
                        Permissions.ViewCourse,
                        Permissions.ViewExercise,
                        Permissions.CreateExercise,
                        Permissions.EditExercise,
                        Permissions.DeleteExercise,
                        Permissions.RunExercise
                });

        public static final SystemRole Student = new SystemRole(
                "Student",
                "Студент",
                PermissionScopeKind.COURSE,
                new SystemPermission[] {
                        Permissions.ViewCourse,
                        Permissions.ViewExercise,
                        Permissions.RunExercise
                });
    }
}
