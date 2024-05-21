package org.vstu.compprehension.models.businesslogic.auth;

public class AuthObjects {

    public static class Permissions {

        // EducationResource
        public static final SystemPermission viewEducationResource = new SystemPermission("ViewEducationResource", "Просмотр обучающих ресурсов");
        public static final SystemPermission createEducationResource = new SystemPermission("CreateEducationResource", "Создание обучающих ресурсов");
        public static final SystemPermission editEducationResource = new SystemPermission("EditEducationResource", "Редактирование обучающих ресурсов");
        public static final SystemPermission deleteEducationResource = new SystemPermission("DeleteEducationResource", "Удаление обучающих ресурсов");

        // Courses
        public static final SystemPermission viewCourse = new SystemPermission("ViewCourse", "Просмотр курсов");
        public static final SystemPermission createCourse = new SystemPermission("CreateCourse", "Создание курсов");
        public static final SystemPermission editCourse = new SystemPermission("EditCourse", "Редактирование курсов");
        public static final SystemPermission deleteCourse = new SystemPermission("DeleteCourse", "Удаление курсов");

        // Exercises
        public static final SystemPermission viewExercise = new SystemPermission("ViewExercise", "Просмотр упражнений");
        public static final SystemPermission createExercise = new SystemPermission("CreateExercise", "Создание упражнений");
        public static final SystemPermission editExercise = new SystemPermission("EditExercise", "Редактирование упражнений");
        public static final SystemPermission deleteExercise = new SystemPermission("DeleteExercise", "Удаление упражнений");
        public static final SystemPermission solveExercise = new SystemPermission("SolveExercise", "Выполнение упражнений");
    }

    public static class Roles {

        public static final SystemRole globalAdmin = new SystemRole("GlobalAdmin", "Глобальный администратор",
                new SystemPermission[]{
                        Permissions.viewEducationResource,
                        Permissions.createEducationResource,
                        Permissions.editEducationResource,
                        Permissions.deleteEducationResource,
                        Permissions.viewCourse,
                        Permissions.editCourse,
                        Permissions.createCourse,
                        Permissions.deleteCourse,
                        Permissions.viewExercise,
                        Permissions.editExercise,
                        Permissions.createExercise,
                        Permissions.deleteExercise,
                        Permissions.solveExercise
        });

        public static final SystemRole educationResourceAdmin = new SystemRole("EducationResourceAdmin", "Администратор обучающего ресурса",
                new SystemPermission[]{
                        Permissions.viewCourse,
                        Permissions.createCourse,
                        Permissions.editCourse,
                        Permissions.deleteCourse,
                        Permissions.viewExercise,
                        Permissions.editExercise,
                        Permissions.createExercise,
                        Permissions.deleteExercise,
                        Permissions.solveExercise
        });

        public static final SystemRole teacher = new SystemRole("Teacher", "Учитель",
                new SystemPermission[]{
                        Permissions.viewCourse,
                        Permissions.createCourse,
                        Permissions.editCourse,
                        Permissions.deleteCourse,
                        Permissions.viewExercise,
                        Permissions.createExercise,
                        Permissions.editExercise,
                        Permissions.deleteExercise,
                        Permissions.solveExercise
        });

        public static final SystemRole assistant = new SystemRole("Assistant", "Ассистент",
                new SystemPermission[]{
                        Permissions.viewCourse,
                        Permissions.viewExercise,
                        Permissions.createExercise,
                        Permissions.editExercise,
                        Permissions.deleteExercise,
                        Permissions.solveExercise
        });

        public static final SystemRole student = new SystemRole("Student", "Студент",
                new SystemPermission[]{
                        Permissions.viewCourse,
                        Permissions.viewExercise,
                        Permissions.solveExercise
        });

        public static final SystemRole guest = new SystemRole("Guest", "Гость",
                new SystemPermission[]{
                        Permissions.viewCourse,
                        Permissions.viewExercise
                });
    }
}

