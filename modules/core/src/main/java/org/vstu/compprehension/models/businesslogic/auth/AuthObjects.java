package org.vstu.compprehension.models.businesslogic.auth;

public class AuthObjects {

    public static class Permissions {

        // EducationResource
        public static final SystemPermission ViewEducationResource = new SystemPermission("ViewEducationResource", "Просмотр обучающих ресурсов");
        public static final SystemPermission CreateEducationResource = new SystemPermission("CreateEducationResource", "Создание обучающих ресурсов");
        public static final SystemPermission EditEducationResource = new SystemPermission("EditEducationResource", "Редактирование обучающих ресурсов");
        public static final SystemPermission DeleteEducationResource = new SystemPermission("DeleteEducationResource", "Удаление обучающих ресурсов");

        // Courses
        public static final SystemPermission ViewCourse = new SystemPermission("ViewCourse", "Просмотр курсов");
        public static final SystemPermission CreateCourse = new SystemPermission("CreateCourse", "Создание курсов");
        public static final SystemPermission EditCourse = new SystemPermission("EditCourse", "Редактирование курсов");
        public static final SystemPermission DeleteCourse = new SystemPermission("DeleteCourse", "Удаление курсов");

        // Exercises
        public static final SystemPermission ViewExercise = new SystemPermission("ViewExercise", "Просмотр упражнений");
        public static final SystemPermission CreateExercise = new SystemPermission("CreateExercise", "Создание упражнений");
        public static final SystemPermission EditExercise = new SystemPermission("EditExercise", "Редактирование упражнений");
        public static final SystemPermission DeleteExercise = new SystemPermission("DeleteExercise", "Удаление упражнений");
        public static final SystemPermission SolveExercise = new SystemPermission("SolveExercise", "Выполнение упражнений");
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

