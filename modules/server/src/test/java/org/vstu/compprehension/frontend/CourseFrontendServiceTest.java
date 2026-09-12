package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.dto.ExerciseDto;
import org.vstu.compprehension.frontend.dto.ExerciseRefDto;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CourseFrontendServiceTest extends AbstractIntegrationTest {

    @Autowired private CourseFrontendService service;
    @Autowired private ExerciseFrontendService exerciseService;

    // ---- курсы пользователя ----

    /** Глобальный админ видит все курсы. */
    @Test
    void getUserCoursesForGlobalAdminReturnsAllCourses() {
        // Act.
        var courses = service.getUserCourses(TestData.Users.GLOBAL_ADMIN_ID);

        // Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID, TestData.Courses.OTHER_ID), ids(courses));
        var main = find(courses, TestData.Courses.MAIN_ID);
        assertEquals("Main test course", main.getName());
        assertEquals(TestData.EducationResources.ID, main.getEducationResourceId());
        assertEquals(TestData.EducationResources.URL, main.getEducationResourceUrl());
    }

    /** Преподаватель видит только свой курс. */
    @Test
    void getUserCoursesForTeacherReturnsOwnCourse() {
        // Act.
        var mainTeacher = service.getUserCourses(TestData.Users.MAIN_COURSE_TEACHER_ID);
        var otherTeacher = service.getUserCourses(TestData.Users.OTHER_COURSE_TEACHER_ID);

        // Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID), ids(mainTeacher));
        assertEquals(Set.of(TestData.Courses.OTHER_ID), ids(otherTeacher));
    }

    /** Ассистент видит курс. */
    @Test
    void getUserCoursesForAssistantReturnsCourse() {
        // Act.
        var courses = service.getUserCourses(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID), ids(courses));
    }

    /** Админ образовательного ресурса видит все его курсы. */
    @Test
    void getUserCoursesForEducationResourceAdminReturnsResourceCourses() {
        // Act.
        var courses = service.getUserCourses(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID);

        // Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID, TestData.Courses.OTHER_ID), ids(courses));
    }

    /** Без VIEW_COURSE список пуст. */
    @Test
    void getUserCoursesWithoutViewPermissionIsEmpty() {
        // Act & Assert.
        assertTrue(service.getUserCourses(TestData.Users.GLOBAL_STUDENT_ID).isEmpty());
        assertTrue(service.getUserCourses(TestData.Users.MAIN_COURSE_STUDENT_ID).isEmpty());
        assertTrue(service.getUserCourses(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID).isEmpty());
        assertTrue(service.getUserCourses(TestData.Users.WITHOUT_ROLES_ID).isEmpty());
    }

    // ---- членство упражнений ----

    /** Курсы, в которые входит упражнение. */
    @Test
    void getExerciseMembershipsReturnsLinkedCourses() {
        // Act & Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID), ids(service.getExerciseMemberships(TestData.Exercises.INHERITED_ID)));
        assertEquals(Set.of(TestData.Courses.MAIN_ID), ids(service.getExerciseMemberships(TestData.Exercises.MAIN_COURSE_ID)));
        assertEquals(Set.of(TestData.Courses.OTHER_ID), ids(service.getExerciseMemberships(TestData.Exercises.OTHER_COURSE_ID)));
        assertTrue(service.getExerciseMemberships(TestData.Exercises.GLOBAL_POOL_ID).isEmpty());
    }

    /** Публичное упражнение подключается к курсу. */
    @Test
    void addExerciseToCourseLinksPublicExercise() {
        // Act.
        service.addExerciseToCourse(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID);

        // Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID), ids(service.getExerciseMemberships(TestData.Exercises.GLOBAL_POOL_ID)));
        assertTrue(exerciseIds(exerciseService.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).exercises())
                .contains(TestData.Exercises.GLOBAL_POOL_ID));
        assertTrue(exerciseService.getExerciseCard(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID)
                .getPermissions().canUnlinkFromCourse());
    }

    /** Повторное подключение ничего не меняет. */
    @Test
    void addExerciseToCourseTwiceIsIdempotent() {
        // Arrange.
        service.addExerciseToCourse(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID);

        // Act.
        assertDoesNotThrow(() -> service.addExerciseToCourse(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID));

        // Assert.
        assertEquals(1, service.getExerciseMemberships(TestData.Exercises.GLOBAL_POOL_ID).size());
    }

    /** Приватное упражнение к другому курсу не подключить. */
    @Test
    void addExerciseToCourseRejectsPrivateExercise() {
        // Act & Assert.
        var error = assertThrows(IllegalStateException.class,
                () -> service.addExerciseToCourse(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.OTHER_ID));
        assertEquals("source_not_in_global_pool", error.getMessage());
        assertEquals(Set.of(TestData.Courses.MAIN_ID), ids(service.getExerciseMemberships(TestData.Exercises.MAIN_COURSE_ID)));
    }

    /** Отвязка оставляет упражнение в пуле. */
    @Test
    void removeExerciseFromCourseUnlinksButKeepsExercise() {
        // Act.
        service.removeExerciseFromCourse(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID);

        // Assert.
        assertTrue(service.getExerciseMemberships(TestData.Exercises.INHERITED_ID).isEmpty());
        assertFalse(exerciseIds(exerciseService.listExercises(TestData.Courses.MAIN_ID, TestData.Users.MAIN_COURSE_TEACHER_ID).exercises())
                .contains(TestData.Exercises.INHERITED_ID));
        assertDoesNotThrow(() -> exerciseService.getExerciseCard(TestData.Exercises.INHERITED_ID, null, TestData.Users.GLOBAL_ADMIN_ID));
    }

    /** Отвязка непривязанного не падает. */
    @Test
    void removeExerciseFromCourseWithoutLinkIsNoop() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.removeExerciseFromCourse(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID));
        assertTrue(service.getExerciseMemberships(TestData.Exercises.GLOBAL_POOL_ID).isEmpty());
    }

    /** Служебная привязка не проверяет публичность. */
    @Test
    void linkExerciseWithCourseIfMissingLinksPrivateExercise() {
        // Act.
        service.linkExerciseWithCourseIfMissing(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.OTHER_ID);

        // Assert.
        assertEquals(Set.of(TestData.Courses.MAIN_ID, TestData.Courses.OTHER_ID), ids(service.getExerciseMemberships(TestData.Exercises.MAIN_COURSE_ID)));
    }

    // ---- ссылки на упражнения курса ----

    /** Ссылки на упражнения курса. */
    @Test
    void getExerciseRefsInCourseReturnsNames() {
        // Act.
        var refs = service.getExerciseRefsInCourseOrThrow(TestData.Courses.MAIN_ID,
                List.of(TestData.Exercises.MAIN_COURSE_ID, TestData.Exercises.INHERITED_ID));

        // Assert.
        assertEquals(2, refs.size());
        assertEquals("Main course exercise", findRef(refs, TestData.Exercises.MAIN_COURSE_ID).name());
        assertEquals("Inherited exercise", findRef(refs, TestData.Exercises.INHERITED_ID).name());
    }

    /** Пустой запрос — пустой ответ. */
    @Test
    void getExerciseRefsInCourseForNoIdsIsEmpty() {
        // Act & Assert.
        assertTrue(service.getExerciseRefsInCourseOrThrow(TestData.Courses.MAIN_ID, List.of()).isEmpty());
    }

    /** Повторы в запросе схлопываются. */
    @Test
    void getExerciseRefsInCourseIgnoresDuplicateIds() {
        // Act.
        var refs = service.getExerciseRefsInCourseOrThrow(TestData.Courses.MAIN_ID,
                List.of(TestData.Exercises.MAIN_COURSE_ID, TestData.Exercises.MAIN_COURSE_ID));

        // Assert.
        assertEquals(1, refs.size());
    }

    /** Чужое упражнение в запросе — ошибка с его id. */
    @Test
    void getExerciseRefsInCourseFailsForExerciseOutsideCourse() {
        // Act & Assert.
        var error = assertThrows(IllegalArgumentException.class,
                () -> service.getExerciseRefsInCourseOrThrow(TestData.Courses.MAIN_ID,
                        List.of(TestData.Exercises.MAIN_COURSE_ID, TestData.Exercises.OTHER_COURSE_ID)));
        assertTrue(error.getMessage().contains(String.valueOf(TestData.Exercises.OTHER_COURSE_ID)));
    }

    // ---- курсы из LTI ----

    /** Поиск по внешнему id. */
    @Test
    void findCourseIdByExternalIdAndResourceId() {
        // Act & Assert.
        assertEquals(Optional.of(TestData.Courses.MAIN_ID),
                service.findCourseIdByExternalIdAndResourceId(TestData.Courses.MAIN_EXTERNAL_ID, TestData.EducationResources.ID));
        assertEquals(Optional.of(TestData.Courses.OTHER_ID),
                service.findCourseIdByExternalIdAndResourceId(TestData.Courses.OTHER_EXTERNAL_ID, TestData.EducationResources.ID));
        assertEquals(Optional.empty(),
                service.findCourseIdByExternalIdAndResourceId("no-such-course", TestData.EducationResources.ID));
        assertEquals(Optional.empty(),
                service.findCourseIdByExternalIdAndResourceId(TestData.Courses.MAIN_EXTERNAL_ID, Long.MIN_VALUE));
    }

    /** Существующий курс из LTI-контекста. */
    @Test
    void resolveFromLtiContextFindsExistingCourse() {
        // Act.
        var courseId = service.resolveOrCreateIdFromLtiContext(
                ltiContext(new LtiCourseContext(TestData.Courses.MAIN_EXTERNAL_ID, "Renamed in LMS")), TestData.EducationResources.ID);

        // Assert.
        assertEquals(Optional.of(TestData.Courses.MAIN_ID), courseId);
        assertEquals("Main test course", find(service.getUserCourses(TestData.Users.GLOBAL_ADMIN_ID), TestData.Courses.MAIN_ID).getName());
    }

    /** Новый курс создаётся из LTI-контекста. */
    @Test
    void resolveFromLtiContextCreatesMissingCourse() {
        // Act.
        var courseId = service.resolveOrCreateIdFromLtiContext(
                ltiContext(new LtiCourseContext("ext-course-new", "New course")), TestData.EducationResources.ID);

        // Assert.
        assertTrue(courseId.isPresent());
        assertNotEquals(TestData.Courses.MAIN_ID, courseId.get());
        var created = find(service.getUserCourses(TestData.Users.GLOBAL_ADMIN_ID), courseId.get());
        assertEquals("New course", created.getName());
        assertEquals(TestData.EducationResources.ID, created.getEducationResourceId());
        assertEquals(courseId, service.findCourseIdByExternalIdAndResourceId("ext-course-new", TestData.EducationResources.ID));
    }

    /** Повторный запуск возвращает тот же курс. */
    @Test
    void resolveFromLtiContextTwiceReturnsSameCourse() {
        // Arrange.
        var context = ltiContext(new LtiCourseContext("ext-course-new", "New course"));
        var first = service.resolveOrCreateIdFromLtiContext(context, TestData.EducationResources.ID);

        // Act.
        var second = service.resolveOrCreateIdFromLtiContext(context, TestData.EducationResources.ID);

        // Assert.
        assertEquals(first, second);
        assertEquals(3, service.getUserCourses(TestData.Users.GLOBAL_ADMIN_ID).size());
    }

    /** Без имени курс называется по внешнему id. */
    @Test
    void resolveFromLtiContextNamesCourseByExternalIdWhenNameMissing() {
        // Act.
        var courseId = service.resolveOrCreateIdFromLtiContext(
                ltiContext(new LtiCourseContext("ext-course-unnamed", null)), TestData.EducationResources.ID);

        // Assert.
        assertEquals("id_ext-course-unnamed", find(service.getUserCourses(TestData.Users.GLOBAL_ADMIN_ID), courseId.orElseThrow()).getName());
    }

    /** Запуск вне курса. */
    @Test
    void resolveFromLtiContextWithoutCourseIsEmpty() {
        // Act & Assert.
        assertEquals(Optional.empty(), service.resolveOrCreateIdFromLtiContext(ltiContext(null), TestData.EducationResources.ID));
        assertEquals(Optional.empty(), service.resolveOrCreateIdFromLtiContext(ltiContext(new LtiCourseContext(null, "Nameless")), TestData.EducationResources.ID));
        assertEquals(2, service.getUserCourses(TestData.Users.GLOBAL_ADMIN_ID).size());
    }

    // ---- вспомогательное ----

    private static LtiContext ltiContext(LtiCourseContext course) {
        return new LtiContext(null, course, TestData.EducationResources.URL, "Test LMS", EducationResourceType.MOODLE, null);
    }

    private static Set<Long> ids(List<CourseDto> courses) {
        return courses.stream().map(CourseDto::getId).collect(Collectors.toSet());
    }

    private static Set<Long> exerciseIds(List<ExerciseDto> exercises) {
        return exercises.stream().map(ExerciseDto::getId).collect(Collectors.toSet());
    }

    private static CourseDto find(List<CourseDto> courses, long id) {
        return courses.stream().filter(c -> c.getId() == id).findFirst().orElseThrow();
    }

    private static ExerciseRefDto findRef(List<ExerciseRefDto> refs, long exerciseId) {
        return refs.stream().filter(r -> r.exerciseId() == exerciseId).findFirst().orElseThrow();
    }
}
