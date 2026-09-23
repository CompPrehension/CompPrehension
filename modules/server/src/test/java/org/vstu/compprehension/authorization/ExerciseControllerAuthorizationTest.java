package org.vstu.compprehension.authorization;

import org.vstu.compprehension.controllers.ExerciseController;
import org.vstu.compprehension.infrastructure.TestData;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

class ExerciseControllerAuthorizationTest extends AbstractAuthorizationTest {

    /** Параметры упражнения нужны для решения, студенту курса доступны. */
    @Test
    void shortInfoAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseShortInfo(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Права преподавателя не переносятся в соседний курс. */
    @Test
    void shortInfoForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.Users.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseShortInfo(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без ролей доступа нет. */
    @Test
    void shortInfoForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.Users.WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseShortInfo(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Приватное упражнение курса вне контекста курса недоступно. */
    @Test
    void shortInfoOfCourseExerciseRejectedWithoutCourseContext() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseShortInfo(TestData.Exercises.MAIN_COURSE_ID, null)).build().toUri()));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** По приватному упражнению курса нельзя завести попытку вне контекста курса. */
    @Test
    void attemptOnCourseExerciseRejectedWithoutCourseContext() throws Exception {
        // Arrange.
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .createExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, null)).build().toUri()));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Поиск своей незавершённой попытки студенту курса разрешён. */
    @Test
    void getExistingAttemptAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExistingExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Чужой курс закрыт и на чтение попыток. */
    @Test
    void getExistingAttemptForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.Users.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExistingExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Студент начинает решать упражнение своего курса. */
    @Test
    void createAttemptAllowedForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .createExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Без ролей нельзя начать решать. */
    @Test
    void createAttemptForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.Users.WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .createExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Чужой exerciseId со своим courseId отсекается проверкой связи упражнения с курсом. */
    @Test
    void createAttemptRejectedForExerciseOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .createExerciseAttempt(TestData.Exercises.OTHER_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Отладочная попытка требует CREATE_DEBUG_ATTEMPT. */
    @Test
    void createDebugAttemptForbiddenForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .createDebugExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** У ассистента CREATE_DEBUG_ATTEMPT нет. */
    @Test
    void createDebugAttemptForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .createDebugExerciseAttempt(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID)).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Владелец читает свою попытку. */
    @Test
    void getAttemptAllowedForItsOwner() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseAttempt(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Преподаватель читает чужие попытки своего курса по VIEW_OTHER_ATTEMPTS. */
    @Test
    void getAttemptAllowedForCourseTeacher() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseAttempt(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Ассистент читает чужие попытки своего курса по VIEW_OTHER_ATTEMPTS. */
    @Test
    void getAttemptAllowedForCourseAssistant() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseAttempt(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Студент не видит попыток других студентов. */
    @Test
    void getAttemptForbiddenForAnotherStudent() throws Exception {
        // Arrange.
        var attempt = createMainCourseAttempt();
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseAttempt(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Попытка вне курса проверяется в GLOBAL-области. */
    @Test
    void getGlobalPoolAttemptAllowedForItsOwner() throws Exception {
        // Arrange.
        var attempt = createGlobalPoolAttempt();
        actingAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get(fromMethodCall(on(ExerciseController.class)
                .getExerciseAttempt(attempt.getId())).build().toUri()));

        // Assert.
        result.andExpect(status().isOk());
    }
}
