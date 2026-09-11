package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.exercise.ExerciseOptionsData;
import org.vstu.compprehension.enums.RoleInExercise;
import org.vstu.compprehension.frontend.dto.ExerciseCardDto;
import org.vstu.compprehension.frontend.dto.ExerciseDto;
import org.vstu.compprehension.frontend.dto.ExerciseSkillDto;
import org.vstu.compprehension.frontend.dto.ExerciseStageDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class ExerciseFrontendServiceTest extends AbstractIntegrationTest {

    private static final String DT_BACKEND_ID = "DTReasoner";

    @Autowired private ExerciseFrontendService service;
    @Autowired private ExerciseAttemptFrontendService attemptService;

    // ---- карточка ----

    /** Карточка упражнения из глобального пула. */
    @Test
    void getExerciseCardOfGlobalPoolExercise() {
        // Act.
        var card = service.getExerciseCard(TestData.Exercises.GLOBAL_POOL_ID, null, TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Assert.
        assertEquals(TestData.Exercises.GLOBAL_POOL_ID, card.getId());
        assertEquals("Global pool exercise", card.getName());
        assertEquals(TestData.Exercises.DOMAIN_ID, card.getDomainId());
        assertEquals(TestData.Exercises.STRATEGY_ID, card.getStrategyId());
        assertEquals(TestData.Exercises.BACKEND_ID, card.getBackendId());
        assertTrue(card.isPublic());
        assertTrue(card.getTags().isEmpty());
        assertTrue(card.getStages().isEmpty());
        assertEquals(new ExerciseOptionsData(), card.getOptions());
    }

    /** Автор пула правит, но не удаляет. */
    @Test
    void globalAuthorCanEditButNotDeletePoolExercise() {
        // Act.
        var permissions = service.getExerciseCard(TestData.Exercises.GLOBAL_POOL_ID, null, TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID).getPermissions();

        // Assert.
        assertTrue(permissions.canEdit());
        assertFalse(permissions.canDelete());
        assertFalse(permissions.canCloneToCourse());
        assertFalse(permissions.canCopyToGlobalPool());
        assertFalse(permissions.canUnlinkFromCourse());
    }

    /** Глобальный админ удаляет из пула. */
    @Test
    void globalAdminCanDeletePoolExercise() {
        // Act.
        var permissions = service.getExerciseCard(TestData.Exercises.GLOBAL_POOL_ID, null, TestData.Users.GLOBAL_ADMIN_ID).getPermissions();

        // Assert.
        assertTrue(permissions.canEdit());
        assertTrue(permissions.canDelete());
    }

    /** Студенту карточка только на чтение. */
    @Test
    void studentHasNoCardPermissions() {
        // Act.
        var permissions = service.getExerciseCard(TestData.Exercises.GLOBAL_POOL_ID, null, TestData.Users.GLOBAL_STUDENT_ID).getPermissions();

        // Assert.
        assertFalse(permissions.canEdit());
        assertFalse(permissions.canDelete());
        assertFalse(permissions.canCloneToCourse());
        assertFalse(permissions.canCopyToGlobalPool());
        assertFalse(permissions.canUnlinkFromCourse());
    }

    /** Унаследованное в курсе: клонировать и отвязать, но не править. */
    @Test
    void inheritedExerciseInCourseIsReadOnlyForTeacher() {
        // Act.
        var card = service.getExerciseCard(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Assert.
        assertTrue(card.isPublic());
        var permissions = card.getPermissions();
        assertFalse(permissions.canEdit());
        assertFalse(permissions.canDelete());
        assertTrue(permissions.canCloneToCourse());
        assertFalse(permissions.canCopyToGlobalPool());
        assertTrue(permissions.canUnlinkFromCourse());
    }

    /** Своё упражнение курса преподаватель правит и удаляет. */
    @Test
    void courseExerciseIsEditableForTeacher() {
        // Act.
        var card = service.getExerciseCard(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Assert.
        assertFalse(card.isPublic());
        var permissions = card.getPermissions();
        assertTrue(permissions.canEdit());
        assertTrue(permissions.canDelete());
        assertFalse(permissions.canCloneToCourse());
        assertFalse(permissions.canCopyToGlobalPool());
        assertFalse(permissions.canUnlinkFromCourse());
    }

    /** Копировать в пул может только автор пула. */
    @Test
    void copyToGlobalPoolRequiresGlobalCreatePermission() {
        // Act.
        var teacher = service.getExerciseCard(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).getPermissions();
        var admin = service.getExerciseCard(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID, TestData.Users.GLOBAL_ADMIN_ID).getPermissions();

        // Assert.
        assertFalse(teacher.canCopyToGlobalPool());
        assertTrue(admin.canCopyToGlobalPool());
    }

    /** Упражнение курса недоступно без контекста курса. */
    @Test
    void getExerciseCardOfCourseExerciseWithoutCourseFails() {
        // Act & Assert.
        var error = assertThrows(IllegalStateException.class,
                () -> service.getExerciseCard(TestData.Exercises.MAIN_COURSE_ID, null, TestData.Users.GLOBAL_ADMIN_ID));
        assertEquals("exercise_not_in_global_pool", error.getMessage());
    }

    /** Упражнение не привязано к курсу. */
    @Test
    void getExerciseCardOfExerciseOutsideCourseFails() {
        // Act & Assert.
        assertThrows(IllegalStateException.class,
                () -> service.getExerciseCard(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID));
    }

    /** Несуществующее упражнение. */
    @Test
    void getExerciseCardOfUnknownExerciseFails() {
        // Act & Assert.
        assertThrows(NoSuchElementException.class,
                () -> service.getExerciseCard(Long.MIN_VALUE, null, TestData.Users.GLOBAL_ADMIN_ID));
    }

    /** Краткая информация: id и опции. */
    @Test
    void getExerciseShortInfoReturnsOptions() {
        // Act.
        var info = service.getExerciseShortInfo(TestData.Exercises.EXPRESSION_DT_ID, null);

        // Assert.
        assertEquals(TestData.Exercises.EXPRESSION_DT_ID, info.getId());
        assertTrue(info.getOptions().isNewQuestionGenerationEnabled());
        assertTrue(info.getOptions().isSupplementaryQuestionsEnabled());
        assertTrue(info.getOptions().isCorrectAnswerGenerationEnabled());
        assertTrue(info.getOptions().isDebugButtonEnabled());
        assertFalse(info.getOptions().isForceNewAttemptCreationEnabled());
        assertEquals(1, info.getOptions().getMaxExpectedConcurrentStudents());
    }

    /** Проверка существования в контексте. */
    @Test
    void ensureExerciseExistsChecksContext() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureExerciseExists(TestData.Exercises.GLOBAL_POOL_ID, null));
        assertDoesNotThrow(() -> service.ensureExerciseExists(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID));
        assertThrows(IllegalStateException.class, () -> service.ensureExerciseExists(TestData.Exercises.MAIN_COURSE_ID, null));
        assertThrows(NoSuchElementException.class, () -> service.ensureExerciseExists(Long.MIN_VALUE, null));
    }

    /** Просмотр открыт тем, кому доступен пул или курс. */
    @Test
    void ensureCanViewExerciseChecksPoolAndCourses() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureCanViewExercise(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, TestData.Exercises.GLOBAL_POOL_ID));
        assertDoesNotThrow(() -> service.ensureCanViewExercise(TestData.Users.MAIN_COURSE_TEACHER_ID, TestData.Exercises.MAIN_COURSE_ID));
        assertDoesNotThrow(() -> service.ensureCanViewExercise(TestData.Users.MAIN_COURSE_ASSISTANT_ID, TestData.Exercises.INHERITED_ID));
        assertThrows(SecurityException.class, () -> service.ensureCanViewExercise(TestData.Users.OTHER_COURSE_TEACHER_ID, TestData.Exercises.MAIN_COURSE_ID));
        assertThrows(SecurityException.class, () -> service.ensureCanViewExercise(TestData.Users.GLOBAL_STUDENT_ID, TestData.Exercises.GLOBAL_POOL_ID));
    }

    // ---- список ----

    /** Глобальный пул: только публичные. */
    @Test
    void listExercisesInGlobalPoolReturnsPublicOnes() {
        // Act.
        var list = service.listExercises(null, TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Assert.
        assertEquals(
                Set.of(TestData.Exercises.GLOBAL_POOL_ID, TestData.Exercises.INHERITED_ID, TestData.Exercises.EXPRESSION_DT_ID),
                ids(list.exercises()));
        assertTrue(list.exercises().stream().allMatch(ExerciseDto::isPublic));
        assertTrue(list.permissions().canCreateExercise());
        assertFalse(list.permissions().canImportInherit());
        assertFalse(list.permissions().canImportClone());
    }

    /** Курс: свои и унаследованные. */
    @Test
    void listExercisesInCourseReturnsOwnAndInherited() {
        // Act.
        var list = service.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Assert.
        assertEquals(Set.of(TestData.Exercises.MAIN_COURSE_ID, TestData.Exercises.INHERITED_ID), ids(list.exercises()));
        assertFalse(find(list.exercises(), TestData.Exercises.MAIN_COURSE_ID).isPublic());
        assertTrue(find(list.exercises(), TestData.Exercises.INHERITED_ID).isPublic());
        assertTrue(list.permissions().canCreateExercise());
        assertTrue(list.permissions().canImportInherit());
        assertTrue(list.permissions().canImportClone());
    }

    /** Ассистенту список только на чтение. */
    @Test
    void listExercisesGivesAssistantNoPermissions() {
        // Act.
        var permissions = service.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_ASSISTANT_ID).permissions();

        // Assert.
        assertFalse(permissions.canCreateExercise());
        assertFalse(permissions.canImportInherit());
        assertFalse(permissions.canImportClone());
    }

    /** Другой курс: свои упражнения. */
    @Test
    void listExercisesInOtherCourse() {
        // Act.
        var list = service.listExercises(TestData.Courses.OTHER_ID, TestData.Users.OTHER_COURSE_TEACHER_ID);

        // Assert.
        assertEquals(Set.of(TestData.Exercises.OTHER_COURSE_ID), ids(list.exercises()));
    }

    // ---- создание ----

    /** Новое упражнение в пуле: публичное, с настройками по умолчанию. */
    @Test
    void createExerciseInGlobalPool() {
        // Act.
        var id = service.createExerciseAndGetId("Pool exercise", TestData.Exercises.DOMAIN_ID, TestData.Exercises.STRATEGY_ID, null);

        // Assert.
        var card = service.getExerciseCard(id, null, TestData.Users.GLOBAL_ADMIN_ID);
        assertEquals("Pool exercise", card.getName());
        assertEquals(TestData.Exercises.DOMAIN_ID, card.getDomainId());
        assertEquals(TestData.Exercises.STRATEGY_ID, card.getStrategyId());
        assertEquals(DT_BACKEND_ID, card.getBackendId());
        assertTrue(card.isPublic());
        assertTrue(card.getTags().isEmpty());
        assertEquals(1, card.getStages().size());
        assertEquals(5, card.getStages().getFirst().getNumberOfQuestions());
        assertEquals(0.5f, card.getStages().getFirst().getComplexity());
        assertTrue(card.getOptions().isNewQuestionGenerationEnabled());
        assertTrue(card.getOptions().isSupplementaryQuestionsEnabled());
        assertTrue(card.getOptions().isCorrectAnswerGenerationEnabled());
        assertTrue(card.getOptions().isForceNewAttemptCreationEnabled());
        assertFalse(card.getOptions().isDebugButtonEnabled());
        assertTrue(ids(service.listExercises(null, TestData.Users.GLOBAL_ADMIN_ID).exercises()).contains(id));
    }

    /** Новое упражнение в курсе: приватное и привязанное. */
    @Test
    void createExerciseInCourse() {
        // Act.
        var id = service.createExerciseAndGetId("Course exercise", TestData.Exercises.DOMAIN_ID, TestData.Exercises.STRATEGY_ID, TestData.Courses.MAIN_ID);

        // Assert.
        var card = service.getExerciseCard(id, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID);
        assertFalse(card.isPublic());
        assertTrue(card.getPermissions().canEdit());
        assertTrue(ids(service.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).exercises()).contains(id));
        assertFalse(ids(service.listExercises(null, TestData.Users.GLOBAL_ADMIN_ID).exercises()).contains(id));
    }

    /** Неизвестный домен. */
    @Test
    void createExerciseWithUnknownDomainFails() {
        // Act & Assert.
        assertThrows(RuntimeException.class,
                () -> service.createExerciseAndGetId("Broken", "NoSuchDomain", TestData.Exercises.STRATEGY_ID, null));
    }

    // ---- сохранение карточки ----

    /** Сохранение карточки меняет все поля. */
    @Test
    void saveExerciseCardUpdatesEveryField() {
        // Arrange.
        var id = service.createExerciseAndGetId("Before", TestData.Exercises.DOMAIN_ID, TestData.Exercises.STRATEGY_ID, null);
        var stage = ExerciseStageDto.builder()
                .numberOfQuestions(2)
                .complexity(0.3f)
                .laws(List.of())
                .concepts(List.of())
                .skills(List.of(ExerciseSkillDto.builder().name("order_determined_by_precedence").kind(RoleInExercise.TARGETED).build()))
                .build();
        var options = ExerciseOptionsData.builder()
                .newQuestionGenerationEnabled(false)
                .supplementaryQuestionsEnabled(false)
                .correctAnswerGenerationEnabled(true)
                .debugButtonEnabled(true)
                .forceNewAttemptCreationEnabled(false)
                .maxExpectedConcurrentStudents(3)
                .build();

        // Act.
        service.saveExerciseCard(ExerciseCardDto.builder()
                .id(id)
                .name("After")
                .domainId(TestData.Exercises.DOMAIN_ID)
                .strategyId(TestData.Exercises.STRATEGY_ID)
                .backendId("ignored")
                .tags(List.of("C++", "basics"))
                .stages(List.of(stage))
                .options(options)
                .build(), null);

        // Assert.
        var card = service.getExerciseCard(id, null, TestData.Users.GLOBAL_ADMIN_ID);
        assertEquals("After", card.getName());
        assertEquals(DT_BACKEND_ID, card.getBackendId());
        assertEquals(List.of("C++", "basics"), card.getTags());
        assertEquals(1, card.getStages().size());
        assertEquals(2, card.getStages().getFirst().getNumberOfQuestions());
        assertEquals(0.3f, card.getStages().getFirst().getComplexity());
        assertEquals("order_determined_by_precedence", card.getStages().getFirst().getSkills().getFirst().getName());
        assertEquals(RoleInExercise.TARGETED, card.getStages().getFirst().getSkills().getFirst().getKind());
        assertEquals(options, card.getOptions());
        assertTrue(card.isPublic());
    }

    /** Унаследованное упражнение в курсе не сохранить. */
    @Test
    void saveExerciseCardOfInheritedExerciseFails() {
        // Arrange.
        var card = service.getExerciseCard(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act & Assert.
        var error = assertThrows(IllegalStateException.class, () -> service.saveExerciseCard(card, TestData.Courses.MAIN_ID));
        assertEquals("inherited_exercise_is_read_only", error.getMessage());
    }

    /** Публичное упражнение сохраняется из пула. */
    @Test
    void saveExerciseCardOfPoolExerciseFromGlobalPool() {
        // Arrange.
        var card = service.getExerciseCard(TestData.Exercises.INHERITED_ID, null, TestData.Users.GLOBAL_ADMIN_ID);

        // Act.
        service.saveExerciseCard(ExerciseCardDto.builder()
                .id(card.getId())
                .name("Renamed inherited")
                .domainId(card.getDomainId())
                .strategyId(card.getStrategyId())
                .backendId(card.getBackendId())
                .tags(card.getTags())
                .stages(card.getStages())
                .options(card.getOptions())
                .build(), null);

        // Assert.
        assertEquals("Renamed inherited",
                service.getExerciseCard(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).getName());
    }

    // ---- клонирование ----

    /** Копия упражнения курса в пул: публичная, с теми же настройками. */
    @Test
    void cloneCourseExerciseToGlobalPool() {
        // Arrange.
        var source = service.getExerciseCard(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID, TestData.Users.GLOBAL_ADMIN_ID);

        // Act.
        var cloneId = service.cloneExerciseAndGetId(TestData.Exercises.MAIN_COURSE_ID, null);

        // Assert.
        assertNotEquals(source.getId(), cloneId);
        var clone = service.getExerciseCard(cloneId, null, TestData.Users.GLOBAL_ADMIN_ID);
        assertTrue(clone.isPublic());
        assertEquals(source.getName(), clone.getName());
        assertEquals(source.getDomainId(), clone.getDomainId());
        assertEquals(source.getStrategyId(), clone.getStrategyId());
        assertEquals(source.getBackendId(), clone.getBackendId());
        assertEquals(source.getTags(), clone.getTags());
        assertEquals(source.getStages().size(), clone.getStages().size());
        assertEquals(source.getOptions(), clone.getOptions());
        assertFalse(service.getExerciseCard(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID, TestData.Users.GLOBAL_ADMIN_ID).isPublic());
        assertTrue(ids(service.listExercises(null, TestData.Users.GLOBAL_ADMIN_ID).exercises()).contains(cloneId));
    }

    /** Копия из пула в курс: приватная и привязанная к курсу. */
    @Test
    void clonePoolExerciseIntoCourse() {
        // Act.
        var cloneId = service.cloneExerciseAndGetId(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID);

        // Assert.
        var clone = service.getExerciseCard(cloneId, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID);
        assertFalse(clone.isPublic());
        assertEquals("Global pool exercise", clone.getName());
        assertTrue(clone.getPermissions().canEdit());
        assertTrue(ids(service.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).exercises()).contains(cloneId));
        assertFalse(ids(service.listExercises(null, TestData.Users.GLOBAL_ADMIN_ID).exercises()).contains(cloneId));
    }

    /** Дубликат в том же курсе запрещён. */
    @Test
    void cloneCourseExerciseIntoSameCourseFails() {
        // Act & Assert.
        var error = assertThrows(IllegalStateException.class,
                () -> service.cloneExerciseAndGetId(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID));
        assertEquals("duplicating_in_same_course", error.getMessage());
    }

    /** Из курса в курс — только через пул. */
    @Test
    void cloneCourseExerciseIntoOtherCourseFails() {
        // Act & Assert.
        var error = assertThrows(IllegalStateException.class,
                () -> service.cloneExerciseAndGetId(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.OTHER_ID));
        assertTrue(error.getMessage().startsWith("course_to_course_forbidden"));
    }

    // ---- удаление ----

    /** Удаление из пула. */
    @Test
    void deleteExerciseFromGlobalPool() {
        // Arrange.
        var id = service.createExerciseAndGetId("Doomed", TestData.Exercises.DOMAIN_ID, TestData.Exercises.STRATEGY_ID, null);

        // Act.
        service.deleteExercise(id, null);

        // Assert.
        assertThrows(NoSuchElementException.class, () -> service.getExerciseCard(id, null, TestData.Users.GLOBAL_ADMIN_ID));
        assertFalse(ids(service.listExercises(null, TestData.Users.GLOBAL_ADMIN_ID).exercises()).contains(id));
    }

    /** Удаление упражнения курса. */
    @Test
    void deleteCourseExercise() {
        // Arrange.
        var id = service.createExerciseAndGetId("Doomed", TestData.Exercises.DOMAIN_ID, TestData.Exercises.STRATEGY_ID, TestData.Courses.MAIN_ID);

        // Act.
        service.deleteExercise(id, TestData.Courses.MAIN_ID);

        // Assert.
        assertThrows(NoSuchElementException.class, () -> service.getExerciseCard(id, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID));
        assertFalse(ids(service.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).exercises()).contains(id));
    }

    /** Удаление уносит попытки. */
    @Test
    void deleteExerciseRemovesItsAttempts() {
        // Arrange.
        var id = service.createExerciseAndGetId("Doomed", TestData.Exercises.DOMAIN_ID, TestData.Exercises.STRATEGY_ID, null);
        var attempt = attemptService.createExerciseAttempt(id, TestData.Users.GLOBAL_STUDENT_ID, null);

        // Act.
        service.deleteExercise(id, null);

        // Assert.
        assertNull(attemptService.getExerciseAttempt(attempt.getAttemptId()));
    }

    /** Унаследованное из курса не удалить. */
    @Test
    void deleteInheritedExerciseFromCourseFails() {
        // Act & Assert.
        var error = assertThrows(IllegalStateException.class,
                () -> service.deleteExercise(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID));
        assertEquals("inherited_exercise_is_read_only", error.getMessage());
        assertDoesNotThrow(() -> service.getExerciseCard(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID));
    }

    /** Удаление публичного из пула оставляет курсу приватную копию с его попытками. */
    @Test
    void deletePublicExerciseLeavesPrivateCopyInLinkedCourse() {
        // Arrange.
        var attempt = attemptService.createExerciseAttempt(TestData.Exercises.INHERITED_ID, TestData.Users.MAIN_COURSE_STUDENT_ID, TestData.Courses.MAIN_ID);

        // Act.
        service.deleteExercise(TestData.Exercises.INHERITED_ID, null);

        // Assert.
        assertThrows(NoSuchElementException.class, () -> service.getExerciseCard(TestData.Exercises.INHERITED_ID, null, TestData.Users.GLOBAL_ADMIN_ID));
        var courseExercises = service.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).exercises();
        var copy = courseExercises.stream()
                .filter(e -> e.getName().equals("Inherited exercise"))
                .findFirst().orElseThrow();
        assertNotEquals(TestData.Exercises.INHERITED_ID, copy.getId());
        assertFalse(copy.isPublic());
        assertEquals(Set.of(TestData.Exercises.MAIN_COURSE_ID, copy.getId()), ids(courseExercises));
        assertEquals(copy.getId(), attemptService.getExerciseAttempt(attempt.getAttemptId()).getExerciseId());
    }

    // ---- вспомогательное ----

    private static Set<Long> ids(List<ExerciseDto> exercises) {
        return exercises.stream().map(ExerciseDto::getId).collect(Collectors.toSet());
    }

    private static ExerciseDto find(List<ExerciseDto> exercises, long id) {
        return exercises.stream().filter(e -> e.getId() == id).findFirst().orElseThrow();
    }
}
