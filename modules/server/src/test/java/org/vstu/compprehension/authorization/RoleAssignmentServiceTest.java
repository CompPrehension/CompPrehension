package org.vstu.compprehension.authorization;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.Service.AuthScopeFactory;
import org.vstu.compprehension.Service.AuthService;
import org.vstu.compprehension.Service.RoleAssignmentService;
import org.vstu.compprehension.Service.RoleAssignmentService.CourseRoleAssignment;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.models.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.models.entities.EnumData.PermissionScopeKind;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.EducationResourceRepository;
import org.vstu.compprehension.models.repository.PermissionScopeRepository;
import org.vstu.compprehension.models.repository.RoleUserAssignmentRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты RoleAssignmentService.
 */
@Transactional
class RoleAssignmentServiceTest extends AbstractIntegrationTest {

    @Autowired private RoleAssignmentService roleAssignmentService;
    @Autowired private RoleUserAssignmentRepository ruaRepository;
    @Autowired private PermissionScopeRepository scopeRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EducationResourceRepository educationResourceRepository;
    @Autowired private AuthService authService;
    @Autowired private AuthScopeFactory authScopes;

    @PersistenceContext private EntityManager entityManager;

    /** Глобальная роль появляется и сразу действует на проверках прав. */
    @Test
    void assignGlobalRoleGrantsPermissionsOfThatRole() {
        // Arrange.
        assertFalse(authService.isAuthorized(
                TestData.USER_WITHOUT_ROLES_ID, SystemPermission.SOLVE_EXERCISE, authScopes.global()));

        // Act.
        roleAssignmentService.assignGlobalRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.STUDENT);
        resetPersistenceContext();

        // Assert.
        assertTrue(hasRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.STUDENT, PermissionScopeKind.GLOBAL, null));
        assertTrue(authService.isAuthorized(
                TestData.USER_WITHOUT_ROLES_ID, SystemPermission.SOLVE_EXERCISE, authScopes.global()));
    }

    /** Повторная выдача той же роли не создаёт второго назначения. */
    @Test
    void assignGlobalRoleIsIdempotent() {
        // Arrange.
        roleAssignmentService.assignGlobalRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.STUDENT);

        // Act.
        roleAssignmentService.assignGlobalRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.STUDENT);
        resetPersistenceContext();

        // Assert.
        assertEquals(1, countAssignments(
                TestData.USER_WITHOUT_ROLES_ID, SystemRole.STUDENT, PermissionScopeKind.GLOBAL, null));
    }

    /** Роль курса нельзя выдать глобально. */
    @Test
    void assignGlobalRoleRejectsCourseOnlyRole() {
        assertThrows(IllegalArgumentException.class,
                () -> roleAssignmentService.assignGlobalRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.TEACHER));
    }

    /** Роль в образовательном ресурсе выдаётся. */
    @Test
    void reconcileAssignsRoleInEducationResource() {
        // Act.
        roleAssignmentService.reconcileRoleInEducationResource(
                TestData.USER_WITHOUT_ROLES_ID, TestData.EDUCATION_RESOURCE_ID, SystemRole.EDUCATION_RESOURCE_ADMIN);
        resetPersistenceContext();

        // Assert.
        assertTrue(hasRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.EDUCATION_RESOURCE_ADMIN,
                PermissionScopeKind.EDUCATION_RESOURCE, TestData.EDUCATION_RESOURCE_ID));
    }

    /** Пустая желаемая роль снимает выданную. */
    @Test
    void reconcileWithNullRemovesRoleInEducationResource() {
        // Arrange.
        assertTrue(hasRole(TestData.EDUCATION_RESOURCE_ADMIN_ID, SystemRole.EDUCATION_RESOURCE_ADMIN,
                PermissionScopeKind.EDUCATION_RESOURCE, TestData.EDUCATION_RESOURCE_ID));

        // Act.
        roleAssignmentService.reconcileRoleInEducationResource(
                TestData.EDUCATION_RESOURCE_ADMIN_ID, TestData.EDUCATION_RESOURCE_ID, null);
        resetPersistenceContext();

        // Assert.
        assertFalse(hasRole(TestData.EDUCATION_RESOURCE_ADMIN_ID, SystemRole.EDUCATION_RESOURCE_ADMIN,
                PermissionScopeKind.EDUCATION_RESOURCE, TestData.EDUCATION_RESOURCE_ID));
    }

    /** Уже выданная роль переживает повторную сверку. */
    @Test
    void reconcileKeepsAlreadyDesiredRoleInEducationResource() {
        // Act.
        roleAssignmentService.reconcileRoleInEducationResource(
                TestData.EDUCATION_RESOURCE_ADMIN_ID, TestData.EDUCATION_RESOURCE_ID,
                SystemRole.EDUCATION_RESOURCE_ADMIN);
        resetPersistenceContext();

        // Assert.
        assertEquals(1, countAssignments(TestData.EDUCATION_RESOURCE_ADMIN_ID, SystemRole.EDUCATION_RESOURCE_ADMIN,
                PermissionScopeKind.EDUCATION_RESOURCE, TestData.EDUCATION_RESOURCE_ID));
    }

    /** Роль курса нельзя выдать в образовательном ресурсе. */
    @Test
    void reconcileRejectsRoleNotAllowedInEducationResource() {
        assertThrows(IllegalArgumentException.class,
                () -> roleAssignmentService.reconcileRoleInEducationResource(
                        TestData.USER_WITHOUT_ROLES_ID, TestData.EDUCATION_RESOURCE_ID, SystemRole.TEACHER));
    }

    /** Новое курсовое назначение появляется и сразу действует на проверках прав. */
    @Test
    void courseReconcileInsertsNewAssignment() {
        // Act.
        reconcileMainCourse(
                List.of(TestData.USER_WITHOUT_ROLES_ID),
                List.of(new CourseRoleAssignment(
                        TestData.USER_WITHOUT_ROLES_ID, TestData.MAIN_COURSE_ID, SystemRole.TEACHER)),
                List.of(TestData.MAIN_COURSE_ID));

        // Assert.
        assertTrue(hasRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
        assertTrue(authService.isAuthorized(TestData.USER_WITHOUT_ROLES_ID,
                SystemPermission.MANAGE_COURSE_CONTENT, authScopes.course(TestData.MAIN_COURSE_ID)));
    }

    /** Смена роли в курсе снимает прежнюю. */
    @Test
    void courseReconcileReplacesExistingRole() {
        // Arrange.
        assertTrue(hasRole(TestData.MAIN_COURSE_ASSISTANT_ID, SystemRole.ASSISTANT,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));

        // Act.
        reconcileMainCourse(
                List.of(TestData.MAIN_COURSE_ASSISTANT_ID),
                List.of(new CourseRoleAssignment(
                        TestData.MAIN_COURSE_ASSISTANT_ID, TestData.MAIN_COURSE_ID, SystemRole.TEACHER)),
                List.of(TestData.MAIN_COURSE_ID));

        // Assert.
        assertFalse(hasRole(TestData.MAIN_COURSE_ASSISTANT_ID, SystemRole.ASSISTANT,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
        assertTrue(hasRole(TestData.MAIN_COURSE_ASSISTANT_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
    }

    /** Совпавшее назначение не пересоздаётся. */
    @Test
    void courseReconcileKeepsMatchingAssignment() {
        // Act.
        reconcileMainCourse(
                List.of(TestData.MAIN_COURSE_TEACHER_ID),
                List.of(new CourseRoleAssignment(
                        TestData.MAIN_COURSE_TEACHER_ID, TestData.MAIN_COURSE_ID, SystemRole.TEACHER)),
                List.of(TestData.MAIN_COURSE_ID));

        // Assert.
        assertEquals(1, countAssignments(TestData.MAIN_COURSE_TEACHER_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
    }

    /** Назначение в курсе из списка на вычистку снимается, если его нет среди желаемых. */
    @Test
    void courseReconcileSweepsAssignmentWithoutDesiredEntry() {
        // Act.
        reconcileMainCourse(List.of(TestData.MAIN_COURSE_TEACHER_ID), List.of(), List.of(TestData.MAIN_COURSE_ID));

        // Assert.
        assertFalse(hasRole(TestData.MAIN_COURSE_TEACHER_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
    }

    /** Курс вне списка на вычистку не трогается. */
    @Test
    void courseReconcileDoesNotSweepCourseOutsideSweepList() {
        // Act.
        reconcileMainCourse(List.of(TestData.MAIN_COURSE_TEACHER_ID), List.of(), List.of(TestData.OTHER_COURSE_ID));

        // Assert.
        assertTrue(hasRole(TestData.MAIN_COURSE_TEACHER_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
    }

    /** Пустой список пользователей означает, что сверять нечего. */
    @Test
    void courseReconcileDoesNothingForEmptyUserList() {
        // Act.
        reconcileMainCourse(List.of(), List.of(), List.of(TestData.MAIN_COURSE_ID));

        // Assert.
        assertTrue(hasRole(TestData.MAIN_COURSE_TEACHER_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, TestData.MAIN_COURSE_ID));
    }

    /** Глобальную роль нельзя выдать в курсе - пакетная вставка проверяет это отдельно. */
    @Test
    void courseReconcileRejectsRoleNotAllowedInCourse() {
        assertThrows(IllegalArgumentException.class, () -> reconcileMainCourse(
                List.of(TestData.USER_WITHOUT_ROLES_ID),
                List.of(new CourseRoleAssignment(
                        TestData.USER_WITHOUT_ROLES_ID, TestData.MAIN_COURSE_ID, SystemRole.GLOBAL_ADMIN)),
                List.of(TestData.MAIN_COURSE_ID)));
    }

    /** Курсу без области она заводится по ходу назначения. */
    @Test
    void courseReconcileCreatesMissingCourseScope() {
        // Arrange.
        var eduRes = educationResourceRepository.findById(TestData.EDUCATION_RESOURCE_ID).orElseThrow();
        var course = courseRepository.save(new CourseEntity("ext-course-3", "Third test course", eduRes));
        entityManager.flush();
        assertTrue(scopeRepository
                .findByKindAndScopeItemIdIn(PermissionScopeKind.COURSE, List.of(course.getId())).isEmpty());

        // Act.
        reconcileMainCourse(
                List.of(TestData.USER_WITHOUT_ROLES_ID),
                List.of(new CourseRoleAssignment(
                        TestData.USER_WITHOUT_ROLES_ID, course.getId(), SystemRole.TEACHER)),
                List.of(course.getId()));

        // Assert.
        assertFalse(scopeRepository
                .findByKindAndScopeItemIdIn(PermissionScopeKind.COURSE, List.of(course.getId())).isEmpty());
        assertTrue(hasRole(TestData.USER_WITHOUT_ROLES_ID, SystemRole.TEACHER,
                PermissionScopeKind.COURSE, course.getId()));
    }

    private void reconcileMainCourse(List<Long> userIds,
                                     List<CourseRoleAssignment> desired,
                                     List<Long> coursesToSweep) {
        entityManager.flush();
        roleAssignmentService.reconcileCourseRoleAssignments(
                TestData.EDUCATION_RESOURCE_ID, userIds, desired, coursesToSweep);
        resetPersistenceContext();
    }

    /** Пакетные вставки идут мимо JPA, поэтому перед проверками контекст надо перечитать. */
    private void resetPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private boolean hasRole(long userId, SystemRole role, PermissionScopeKind kind, Long scopeItemId) {
        return ruaRepository.existsRoleInScope(userId, role, kind, scopeItemId);
    }

    private long countAssignments(long userId, SystemRole role, PermissionScopeKind kind, Long scopeItemId) {
        return entityManager.createQuery("""
                        select count(rua)
                        from RoleUserAssignmentEntity rua
                        join rua.role r
                        join rua.permissionScope ps
                        where rua.user.id = :userId
                          and r.name = :role
                          and ps.kind = :kind
                          and coalesce(ps.scopeItemId, 0) = coalesce(:scopeItemId, 0)
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("role", role)
                .setParameter("kind", kind)
                .setParameter("scopeItemId", scopeItemId)
                .getSingleResult();
    }
}
