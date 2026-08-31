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

    /** Автор пула ведёт общий список упражнений, значит видит его. */
    @Test
    void listGlobalExercisesAllowedForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list"));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Студент не листает глобальный пул: у него остался только SOLVE_EXERCISE. */
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

    /** Студент не видит списка упражнений даже в своём курсе — приходит по прямой ссылке. */
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

    /** Роль преподавателя действует только в своём курсе и не переносится на соседний. */
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

    /** Карточка упражнения пула доступна тому, кто пул ведёт. */
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

    /** Карточка с настройками — не для решающего. */
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

    /** Карточка курсового упражнения доступна преподавателю этого курса. */
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

    /** Ради этого роль и вводилась: автор создаёт упражнение в общем пуле. */
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

    /** Создание в пуле закрыто для тех, у кого нет CREATE_EXERCISE. */
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

    /** Ассистент видит курс, но не создаёт в нём упражнения. */
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

    /** Правка упражнения пула закрыта для студента. */
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

    /** Преподаватель правит собственное упражнение курса. */
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

    /** Ассистент не правит упражнения курса: EDIT_EXERCISE у роли нет. */
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

    /** Ключевой тест изоляции: глобальные права не дотягиваются до приватного упражнения курса.
     * Не передав courseId, автор пула проходит проверку прав в GLOBAL-области, и остановить его
     * может только проверка принадлежности упражнения заявленному контексту. */
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

    /** Наследованное из пула упражнение из курса только читается: правка задела бы все курсы,
     * которые его наследуют. Права у преподавателя есть, отказ даёт состояние упражнения. */
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

    /** Удаление из пула оставлено за глобальным администратором. */
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

    /** Фиксирует намеренное отсутствие DELETE_EXERCISE у автора пула: удаление каскадом задевает
     * наследующие курсы и необратимо, а владельца у упражнения пока нет. */
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

    /** Обрыв наследования: преподаватель делает из наследованного упражнения свою копию.
     * Глобальных прав для этого не требуется — источник он видит через свой курс. */
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

    /** Копирование в пул не должно становиться способом прочитать чужое курсовое упражнение:
     * доступ к источнику проверяется отдельно, в его собственном контексте. */
    @Test
    void cloneCourseExerciseToGlobalPoolForbiddenWhenSourceIsNotVisible() throws Exception {
        // Arrange.
        actingAs(TestData.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(post("/api/exercise/" + TestData.MAIN_COURSE_EXERCISE_ID + "/clone"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Аутентифицированный пользователь без единой роли не получает ничего. */
    @Test
    void anyEndpointForbiddenForUserWithoutRoles() throws Exception {
        // Arrange.
        actingAs(TestData.USER_WITHOUT_ROLES_ID);

        // Act.
        var result = mockMvc.perform(get("/api/exercise/list"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /**
     * Флаги списка для преподавателя курса: он и создаёт упражнения, и импортирует из пула
     * обоими способами. canImportInherit держится на MANAGE_COURSE_CONTENT.
     */
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

    /** Ассистент видит список, но ни одной кнопки изменения состава курса не получает. */
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

    /** Вне курса импортировать некуда, поэтому оба флага импорта ложны даже у автора пула. */
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

    /**
     * Наследованное упражнение в курсе: править и удалять нельзя, зато можно оборвать
     * наследование или отвязать. canUnlinkFromCourse держится на MANAGE_COURSE_CONTENT.
     */
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

    /**
     * Собственное упражнение курса: правится и удаляется, но отвязывать и обрывать
     * наследование нечего. В пул не копируется — глобальных прав у преподавателя нет.
     */
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

    /** Автор пула правит упражнение пула, но удалять его не может — DELETE_EXERCISE у роли нет. */
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
