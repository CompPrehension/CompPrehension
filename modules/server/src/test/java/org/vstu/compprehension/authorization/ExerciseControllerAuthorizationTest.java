package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Параметры упражнения нужны для его решения, поэтому студенту курса они доступны. */
    @Test
    void shortInfoAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Права преподавателя в своём курсе не дают доступа к упражнениям чужого курса. */
    @Test
    void shortInfoForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Аутентификация без единой роли не даёт доступа никуда. */
    @Test
    void shortInfoForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * TODO: поправить.
     * <p>
     * Без {@code courseId} контроллер достаёт упражнение не проверяя
     * принадлежность заявленному контексту. Поэтому параметры приватного упражнения чужого курса
     * получает любой, у кого есть SOLVE_EXERCISE в GLOBAL-области, — а он есть у всех.
     * <p>
     * Пока такое поведение было оставлено, чтобы не
     * сломать прямые ссылки на упражнение без курса. Когда решим, как ссылка передаёт курс,
     * здесь должен появиться отказ.
     */
    @Test
    void shortInfoOfCourseExerciseIsReachableWithoutCourseContext() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/shortInfo")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /**
     * TODO: поправить.
     * <p>
     * Посторонний не просто читает упражнение чужого курса, а заводит по нему
     * попытку решения.
     */
    @Test
    void attemptOnCourseExerciseCanBeCreatedWithoutCourseContext() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Поиск незавершённой попытки — часть цикла решения, студенту курса разрешён. */
    @Test
    void getExistingAttemptAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExistingExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Чужой курс закрыт и на чтение попыток. */
    @Test
    void getExistingAttemptForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExistingExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Основной сценарий студента: начать решать упражнение своего курса. */
    @Test
    void createAttemptAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Без ролей нельзя даже начать решать. */
    @Test
    void createAttemptForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * Прав в своём курсе недостаточно, чтобы решать упражнение чужого: подстановка чужого
     * exerciseId к своему courseId отсекается проверкой связи упражнения с курсом.
     */
    @Test
    void createAttemptRejectedForExerciseOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.OTHER_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Отладочная попытка — инструмент автора упражнения, а не решающего: студенту закрыта. */
    @Test
    void createDebugAttemptForbiddenForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createDebugExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ассистент видит курс, но не правит упражнения, поэтому отладочная попытка ему закрыта. */
    @Test
    void createDebugAttemptForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/createDebugExerciseAttempt")
                .param("exerciseId", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Своя попытка доступна владельцу — первая ветка ensureOwnerOrPrivileged. */
    @Test
    void getAttemptAllowedForItsOwner() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isOk());
    }

    /**
     * Преподаватель курса видит чужие попытки в своём курсе — вторая ветка
     * ensureOwnerOrPrivileged, привилегия выводится из EDIT_EXERCISE.
     */
    @Test
    void getAttemptAllowedForCourseTeacher() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isOk());
    }

    /**
     * Ассистенту чужие попытки закрыты: роль в курсе есть, но привилегия завязана именно
     * на EDIT_EXERCISE, которого у него нет.
     */
    @Test
    void getAttemptForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студент не видит попыток других студентов. */
    @Test
    void getAttemptForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Попытка вне курса проверяется в GLOBAL-области: владелец получает свою. */
    @Test
    void getGlobalPoolAttemptAllowedForItsOwner() throws Exception {
        // Arrange.
        var attempt = createGlobalPoolAttempt();
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/getExerciseAttempt")
                .param("attemptId", String.valueOf(attempt.getId())));

        // Assert.
        result.andExpect(status().isOk());
    }
}
