package org.vstu.compprehension.authorization;

import org.vstu.compprehension.infrastructure.TestData;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseStageDto;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExerciseSettingsControllerAuthorizationTest extends AbstractAuthorizationTest {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ExerciseRepository exerciseRepository;

    /** Автор пула видит общий список упражнений. */
    @Test
    void listGlobalExercisesAllowedForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list"));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** У студента остался только SOLVE_EXERCISE. */
    @Test
    void listGlobalExercisesForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Преподаватель видит список упражнений своего курса. */
    @Test
    void listCourseExercisesAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Студент списка упражнений курса не видит. */
    @Test
    void listCourseExercisesForbiddenForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Роль преподавателя не переносится в соседний курс. */
    @Test
    void listCourseExercisesForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Карточка упражнения пула доступна автору пула. */
    @Test
    void getExerciseCardAllowedForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise")
                .param("id", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Карточка с настройками решающему закрыта. */
    @Test
    void getExerciseCardForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise")
                .param("id", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Карточка курсового упражнения доступна преподавателю курса. */
    @Test
    void getCourseExerciseCardAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Автор пула создаёт упражнение в пуле. */
    @Test
    void createGlobalExerciseAllowedForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);
        var body = objectMapper.writeValueAsString(new NewExerciseRequest(
                "new exercise", TestData.DOMAIN_ID, TestData.STRATEGY_ID, null));

        // Act.
        var result = mockMvc.perform(put("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Без CREATE_EXERCISE создать нельзя. */
    @Test
    void createGlobalExerciseForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);
        var body = objectMapper.writeValueAsString(new NewExerciseRequest(
                "new exercise", TestData.DOMAIN_ID, TestData.STRATEGY_ID, null));

        // Act.
        var result = mockMvc.perform(put("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Ассистент упражнений в курсе не создаёт. */
    @Test
    void createCourseExerciseForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);
        var body = objectMapper.writeValueAsString(new NewExerciseRequest(
                "new exercise", TestData.DOMAIN_ID, TestData.STRATEGY_ID, TestData.MAIN_COURSE_ID));

        // Act.
        var result = mockMvc.perform(put("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Автор пула правит упражнения пула. */
    @Test
    void updateGlobalExerciseAllowedForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);
        var body = objectMapper.writeValueAsString(cardOf(TestData.GLOBAL_POOL_EXERCISE_ID));

        // Act.
        var result = mockMvc.perform(post("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Студенту правка закрыта. */
    @Test
    void updateGlobalExerciseForbiddenForGlobalStudent() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_STUDENT_ID);
        var body = objectMapper.writeValueAsString(cardOf(TestData.GLOBAL_POOL_EXERCISE_ID));

        // Act.
        var result = mockMvc.perform(post("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Преподаватель правит своё упражнение курса. */
    @Test
    void updateCourseExerciseAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);
        var body = objectMapper.writeValueAsString(cardOf(TestData.MAIN_COURSE_EXERCISE_ID));

        // Act.
        var result = mockMvc.perform(post("/api/exercise")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** У ассистента EDIT_EXERCISE нет. */
    @Test
    void updateCourseExerciseForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);
        var body = objectMapper.writeValueAsString(cardOf(TestData.MAIN_COURSE_EXERCISE_ID));

        // Act.
        var result = mockMvc.perform(post("/api/exercise")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * Изоляция областей: без courseId проверка идёт в GLOBAL, а упражнение курса отсекается
     * проверкой принадлежности заявленному контексту.
     */
    @Test
    void updateCourseExerciseRejectedForGlobalExerciseAuthorActingOutsideCourse() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);
        var body = objectMapper.writeValueAsString(cardOf(TestData.MAIN_COURSE_EXERCISE_ID));

        // Act.
        var result = mockMvc.perform(post("/api/exercise")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Наследованное упражнение только читается: отказ по состоянию, не по правам. */
    @Test
    void updateInheritedExerciseRejectedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);
        var body = objectMapper.writeValueAsString(cardOf(TestData.INHERITED_EXERCISE_ID));

        // Act.
        var result = mockMvc.perform(post("/api/exercise")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Удаление из пула оставлено за администратором. */
    @Test
    void deleteGlobalExerciseAllowedForGlobalAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_ADMIN_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/exercise")
                .param("id", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** У автора пула DELETE_EXERCISE намеренно нет. */
    @Test
    void deleteGlobalExerciseForbiddenForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/exercise")
                .param("id", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Своё упражнение курса преподаватель удаляет. */
    @Test
    void deleteCourseExerciseAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/exercise")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Студент ничего не удаляет. */
    @Test
    void deleteCourseExerciseForbiddenForCourseStudent() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/exercise")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Удаление не проходит через границу курса. */
    @Test
    void deleteCourseExerciseForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        actingAs(TestData.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/exercise")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Обрыв наследования: преподаватель делает свою копию, глобальных прав не нужно. */
    @Test
    void cloneInheritedExerciseAllowedForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/exercise/" + TestData.INHERITED_EXERCISE_ID + "/clone")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Обрыв наследования создаёт упражнение, поэтому требует CREATE_EXERCISE. */
    @Test
    void cloneInheritedExerciseForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/exercise/" + TestData.INHERITED_EXERCISE_ID + "/clone")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Доступ к источнику копирования проверяется в его собственном контексте. */
    @Test
    void cloneCourseExerciseToGlobalPoolForbiddenWhenSourceIsNotVisible() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(post("/api/exercise/" + TestData.MAIN_COURSE_EXERCISE_ID + "/clone"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без ролей не доступно ничего. */
    @Test
    void anyEndpointForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Флаги списка у преподавателя курса. */
    @Test
    void listPermissionsForCourseTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canCreateExercise").value(true))
                .andExpect(jsonPath("$.permissions.canImportInherit").value(true))
                .andExpect(jsonPath("$.permissions.canImportClone").value(true));
    }

    /** У ассистента флагов изменения состава курса нет. */
    @Test
    void listPermissionsForCourseAssistant() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list")
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canCreateExercise").value(false))
                .andExpect(jsonPath("$.permissions.canImportInherit").value(false))
                .andExpect(jsonPath("$.permissions.canImportClone").value(false));
    }

    /** Вне курса импортировать некуда: оба флага импорта ложны. */
    @Test
    void listPermissionsForGlobalExerciseAuthorOutsideCourse() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canCreateExercise").value(true))
                .andExpect(jsonPath("$.permissions.canImportInherit").value(false))
                .andExpect(jsonPath("$.permissions.canImportClone").value(false));
    }

    /** Флаги карточки наследованного упражнения. */
    @Test
    void cardPermissionsForInheritedExercise() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise")
                .param("id", String.valueOf(TestData.INHERITED_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canEdit").value(false))
                .andExpect(jsonPath("$.permissions.canDelete").value(false))
                .andExpect(jsonPath("$.permissions.canCloneToCourse").value(true))
                .andExpect(jsonPath("$.permissions.canUnlinkFromCourse").value(true))
                .andExpect(jsonPath("$.permissions.canCopyToGlobalPool").value(false));
    }

    /** Флаги карточки собственного упражнения курса. */
    @Test
    void cardPermissionsForOwnCourseExercise() throws Exception {
        // Arrange.
        actingAs(TestData.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise")
                .param("id", String.valueOf(TestData.MAIN_COURSE_EXERCISE_ID))
                .param("courseId", String.valueOf(TestData.MAIN_COURSE_ID)));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canEdit").value(true))
                .andExpect(jsonPath("$.permissions.canDelete").value(true))
                .andExpect(jsonPath("$.permissions.canCloneToCourse").value(false))
                .andExpect(jsonPath("$.permissions.canUnlinkFromCourse").value(false))
                .andExpect(jsonPath("$.permissions.canCopyToGlobalPool").value(false));
    }

    /** Флаги карточки упражнения пула у автора пула. */
    @Test
    void cardPermissionsForGlobalPoolExercise() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise")
                .param("id", String.valueOf(TestData.GLOBAL_POOL_EXERCISE_ID)));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canEdit").value(true))
                .andExpect(jsonPath("$.permissions.canDelete").value(false))
                .andExpect(jsonPath("$.permissions.canCloneToCourse").value(false))
                .andExpect(jsonPath("$.permissions.canUnlinkFromCourse").value(false))
                .andExpect(jsonPath("$.permissions.canCopyToGlobalPool").value(false));
    }

    private ExerciseCardDto cardOf(long exerciseId) {
        var exercise = exerciseRepository.findById(exerciseId).orElseThrow();
        return ExerciseCardDto.builder()
                .id(exercise.getId())
                .name(exercise.getName())
                .domainId(exercise.getDomain().getName())
                .strategyId(exercise.getStrategyId())
                .backendId(exercise.getBackendId())
                .tags(new ArrayList<>())
                .options(exercise.getOptions())
                .stages(List.of(new ExerciseStageDto(5, 0.5f, new ArrayList<>(), new ArrayList<>(), new ArrayList<>())))
                .isPublic(exercise.isPublic())
                .build();
    }

    private record NewExerciseRequest(String name, String domainId, String strategyId, Long courseId) {
    }
}
