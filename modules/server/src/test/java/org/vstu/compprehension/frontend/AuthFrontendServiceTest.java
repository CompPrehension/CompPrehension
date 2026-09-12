package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class AuthFrontendServiceTest extends AbstractIntegrationTest {

    @Autowired private AuthFrontendService service;

    /** Глобальная область: роль пула. */
    @Test
    void ensureAuthorizedInGlobalScope() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, SystemPermission.CREATE_EXERCISE, service.global()));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.GLOBAL_ADMIN_ID, SystemPermission.DELETE_EXERCISE, service.global()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID, SystemPermission.DELETE_EXERCISE, service.global()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.GLOBAL_STUDENT_ID, SystemPermission.CREATE_EXERCISE, service.global()));
    }

    /** Область курса: роль преподавателя действует только в своём курсе. */
    @Test
    void ensureAuthorizedInCourseScope() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemPermission.EDIT_EXERCISE, service.course(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemPermission.EDIT_EXERCISE, service.course(TestData.Courses.OTHER_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemPermission.EDIT_EXERCISE, service.global()));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_ASSISTANT_ID, SystemPermission.EDIT_EXERCISE, service.course(TestData.Courses.MAIN_ID)));
    }

    /** Область курса включает его образовательный ресурс. */
    @Test
    void courseScopeIncludesItsEducationResource() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemPermission.MANAGE_COURSE_CONTENT, service.course(TestData.Courses.MAIN_ID)));
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemPermission.MANAGE_COURSE_CONTENT, service.course(TestData.Courses.OTHER_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID, SystemPermission.MANAGE_COURSE_CONTENT, service.global()));
    }

    /** Право студента: решать, но не редактировать. */
    @Test
    void studentCanOnlySolve() {
        // Act & Assert.
        assertDoesNotThrow(() -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemPermission.SOLVE_EXERCISE, service.course(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemPermission.VIEW_EXERCISE, service.course(TestData.Courses.MAIN_ID)));
        assertThrows(SecurityException.class,
                () -> service.ensureAuthorized(TestData.Users.WITHOUT_ROLES_ID, SystemPermission.SOLVE_EXERCISE, service.global()));
    }

    /** Выбор области по наличию курса. */
    @Test
    void courseOrGlobalPicksScopeByCourseId() {
        // Act & Assert.
        assertEquals(service.global(), service.courseOrGlobal(null));
        assertEquals(service.course(TestData.Courses.MAIN_ID), service.courseOrGlobal(TestData.Courses.MAIN_ID));
        assertNotEquals(service.global(), service.course(TestData.Courses.MAIN_ID));
    }
}
