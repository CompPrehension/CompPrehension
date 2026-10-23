package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemCapability;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

@Transactional
class AuthFrontendServiceTest extends AbstractIntegrationTest {

    @Autowired private AuthFrontendService service;

    /** GLOBAL: роль автора пула и администратор через ROOT. */
    @Test
    void ensureAuthorizedInGlobalScope() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, SystemPermission.CREATE_EXERCISE, service.getGlobalScope()));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.ADMIN_ID, SystemPermission.DELETE_EXERCISE, service.getGlobalScope()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, SystemPermission.DELETE_EXERCISE, service.getGlobalScope()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_STUDENT_ID, SystemPermission.CREATE_EXERCISE, service.getGlobalScope()));
    }

    /** ROOT включает все области, а GLOBAL курсов не включает. */
    @Test
    void rootScopeCoversCoursesAndGlobalDoesNot() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.ADMIN_ID, SystemPermission.LINK_POOL_EXERCISE_TO_COURSE, service.getCourseScope(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, SystemPermission.EDIT_EXERCISE, service.getCourseScope(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_STUDENT_ID, SystemPermission.SOLVE_EXERCISE, service.getCourseScope(TestData.Courses.MAIN_ID)));
    }

    /** Область курса: роль преподавателя действует только в своём курсе. */
    @Test
    void ensureAuthorizedInCourseScope() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemPermission.EDIT_EXERCISE, service.getCourseScope(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemPermission.EDIT_EXERCISE, service.getCourseScope(TestData.Courses.OTHER_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemPermission.EDIT_EXERCISE, service.getGlobalScope()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_ASSISTANT_ID, SystemPermission.EDIT_EXERCISE, service.getCourseScope(TestData.Courses.MAIN_ID)));
    }

    /** Область курса включает его образовательный ресурс. */
    @Test
    void courseScopeIncludesItsEducationResource() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemPermission.LINK_POOL_EXERCISE_TO_COURSE, service.getCourseScope(TestData.Courses.MAIN_ID)));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemPermission.LINK_POOL_EXERCISE_TO_COURSE, service.getCourseScope(TestData.Courses.OTHER_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemPermission.LINK_POOL_EXERCISE_TO_COURSE, service.getGlobalScope()));
    }

    /** Право студента: решать, но не редактировать. */
    @Test
    void studentCanOnlySolve() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemPermission.SOLVE_EXERCISE, service.getCourseScope(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemPermission.VIEW_EXERCISE_CARD, service.getCourseScope(TestData.Courses.MAIN_ID)));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.GLOBAL_STUDENT_ID, SystemPermission.SOLVE_EXERCISE, service.getGlobalScope()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.WITHOUT_ROLES_ID, SystemPermission.SOLVE_EXERCISE, service.getGlobalScope()));
    }

    /** Способность засчитывается по роли в любой области. */
    @Test
    void capabilityIgnoresAssignmentScope() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemCapability.DEBUG_BANK_QUESTION));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemCapability.DEBUG_BANK_QUESTION));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, SystemCapability.DEBUG_BANK_QUESTION));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_ASSISTANT_ID, SystemCapability.DEBUG_BANK_QUESTION));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_STUDENT_ID, SystemCapability.DEBUG_BANK_QUESTION));
    }

    /** Область упражнения берётся из его контекста, а чужой контекст отвергается. */
    @Test
    void exerciseScopeFollowsExerciseContext() {
        // Act & Assert.
        assertEquals(service.getGlobalScope(), service.getExerciseScope(TestData.Exercises.GLOBAL_POOL_ID, null));
        assertEquals(service.getCourseScope(TestData.Courses.MAIN_ID), service.getExerciseScope(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.MAIN_ID));
        assertEquals(service.getCourseScope(TestData.Courses.MAIN_ID), service.getExerciseScope(TestData.Exercises.INHERITED_ID, TestData.Courses.MAIN_ID));
        var notInPool = assertThrows(IllegalStateException.class, () -> service.getExerciseScope(TestData.Exercises.MAIN_COURSE_ID, null));
        assertEquals("exercise_not_in_global_pool", notInPool.getMessage());
        assertThrows(IllegalStateException.class, () -> service.getExerciseScope(TestData.Exercises.GLOBAL_POOL_ID, TestData.Courses.MAIN_ID));
        assertThrows(IllegalStateException.class, () -> service.getExerciseScope(TestData.Exercises.MAIN_COURSE_ID, TestData.Courses.OTHER_ID));
        assertThrows(NoSuchElementException.class, () -> service.getExerciseScope(Long.MIN_VALUE, null));
    }
}
