package org.vstu.compprehension.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;
import org.vstu.compprehension.businesslogic.auth.Role;
import org.vstu.compprehension.data.lti.LtiCourseMemberData;
import org.vstu.compprehension.entities.external_system.EducationResourceUserEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.infrastructure.TestLtiMembershipProvider;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceUserRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CourseMembershipSyncServiceTest extends AbstractIntegrationTest {

    // Инструмент LMS из data.sql.
    private static final String ISSUER = TestData.EducationResources.URL;
    private static final String CLIENT_ID = "test-client";

    private static final String MAIN_COURSE_MEMBERS = ISSUER + "/mod/lti/services.php/CourseSection/1/bindings/1/memberships";
    private static final String OTHER_COURSE_MEMBERS = ISSUER + "/mod/lti/services.php/CourseSection/2/bindings/1/memberships";

    private static final String INSTRUCTOR = "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";
    private static final String TEACHING_ASSISTANT = "http://purl.imsglobal.org/vocab/lis/v2/membership/Instructor#TeachingAssistant";
    private static final String LEARNER = "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";
    private static final String MENTOR = "http://purl.imsglobal.org/vocab/lis/v2/membership#Mentor";

    @Autowired private CourseMembershipSyncService syncService;
    @Autowired private LtiCourseMembershipDataService membershipService;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private EducationResourceRepository educationResourceRepository;
    @Autowired private EducationResourceUserRepository educationResourceUserRepository;

    @AfterEach
    void resetLms() {
        TestLtiMembershipProvider.reset();
    }

    /**
     * Новый участник курса получает роль по своей роли в LMS: преподаватель, ассистент, студент.
     * Ассистента LMS присылает вместе с ролью Instructor, которую он уточняет.
     */
    @Test
    void newMembersGetCourseRolesByLmsRoles() {
        // Arrange.
        linkToLms(TestData.Users.WITHOUT_ROLES_ID, "8");
        linkToLms(TestData.Users.OTHER_COURSE_TEACHER_ID, "7");
        linkToLms(TestData.Users.GLOBAL_STUDENT_ID, "3");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS,
                active("8", INSTRUCTOR), active("7", LEARNER), active("3", INSTRUCTOR, TEACHING_ASSISTANT));

        // Act.
        syncService.syncAll();

        // Assert.
        assertTrue(hasMainCourseRole(TestData.Users.WITHOUT_ROLES_ID, SystemRole.TEACHER));
        assertTrue(hasMainCourseRole(TestData.Users.OTHER_COURSE_TEACHER_ID, SystemRole.STUDENT));
        assertTrue(hasMainCourseRole(TestData.Users.GLOBAL_STUDENT_ID, SystemRole.ASSISTANT));
        assertFalse(hasMainCourseRole(TestData.Users.GLOBAL_STUDENT_ID, SystemRole.TEACHER));
    }

    /**
     * Роль участника, который уже есть в курсе, синхронизация не меняет: её задаёт запуск.
     * Учитель без права редактирования приходит в списке участников Moodle как Learner и остаётся преподавателем.
     */
    @Test
    void existingMemberKeepsCourseRole() {
        // Arrange.
        linkToLms(TestData.Users.MAIN_COURSE_TEACHER_ID, "4");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS, active("4", LEARNER));

        // Act.
        syncService.syncAll();

        // Assert.
        assertTrue(hasMainCourseRole(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemRole.TEACHER));
        assertFalse(hasMainCourseRole(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemRole.STUDENT));
    }

    /** Наблюдатель за студентом в LMS (например, родитель) не становится преподавателем курса. */
    @Test
    void mentorIsNotCourseTeacher() {
        // Arrange.
        linkToLms(TestData.Users.WITHOUT_ROLES_ID, "8");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS, active("8", MENTOR));

        // Act.
        syncService.syncAll();

        // Assert.
        assertFalse(hasMainCourseRole(TestData.Users.WITHOUT_ROLES_ID, SystemRole.TEACHER));
        assertTrue(hasMainCourseRole(TestData.Users.WITHOUT_ROLES_ID, SystemRole.STUDENT));
    }

    /** Участник, которого LMS повторила в списке, получает роль, а курс синхронизируется. */
    @Test
    void repeatedMemberGetsCourseRole() {
        // Arrange.
        linkToLms(TestData.Users.WITHOUT_ROLES_ID, "8");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS, active("8", LEARNER), active("8", LEARNER));

        // Act.
        syncService.syncAll();

        // Assert.
        assertTrue(hasMainCourseRole(TestData.Users.WITHOUT_ROLES_ID, SystemRole.STUDENT));
    }

    /** Отчисленный в LMS и пропавший из списка участника теряют роль в курсе. */
    @Test
    void inactiveAndMissingMembersLoseCourseRole() {
        // Arrange.
        linkToLms(TestData.Users.MAIN_COURSE_STUDENT_ID, "6");
        linkToLms(TestData.Users.MAIN_COURSE_ASSISTANT_ID, "5");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS, new LtiCourseMemberData("6", List.of(LEARNER), false));

        // Act.
        syncService.syncAll();

        // Assert.
        assertFalse(hasMainCourseRole(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemRole.STUDENT));
        assertFalse(hasMainCourseRole(TestData.Users.MAIN_COURSE_ASSISTANT_ID, SystemRole.ASSISTANT));
    }

    /**
     * Синхронизация не заводит учёток для незнакомых участников LMS и не трогает роли тех,
     * кто в курс попал не через LMS.
     */
    @Test
    void usersOutsideLmsAreLeftAlone() {
        // Arrange.
        long usersBefore = userRepository.count();
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS, active("never-launched", INSTRUCTOR));

        // Act.
        syncService.syncAll();

        // Assert.
        assertEquals(usersBefore, userRepository.count());
        assertTrue(hasMainCourseRole(TestData.Users.MAIN_COURSE_TEACHER_ID, SystemRole.TEACHER));
    }

    /** Если LMS не отдала список курса, роли в нём остаются прежними, а другие курсы синхронизируются. */
    @Test
    void unavailableLmsKeepsCourseRolesAndOtherCoursesAreSynced() {
        // Arrange.
        linkToLms(TestData.Users.MAIN_COURSE_STUDENT_ID, "6");
        linkToLms(TestData.Users.WITHOUT_ROLES_ID, "8");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        membershipService.rememberSource(TestData.Courses.OTHER_ID, ISSUER, CLIENT_ID, OTHER_COURSE_MEMBERS);
        TestLtiMembershipProvider.isUnavailable(MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(OTHER_COURSE_MEMBERS, active("8", LEARNER));

        // Act.
        syncService.syncAll();

        // Assert.
        assertTrue(hasMainCourseRole(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemRole.STUDENT));
        assertTrue(authService.hasRole(TestData.Users.WITHOUT_ROLES_ID, SystemRole.STUDENT,
                PermissionScope.course(TestData.Courses.OTHER_ID)));
    }

    /** Курс, чей инструмент LMS отключили, больше не синхронизируется: роли в нём не меняются. */
    @Test
    void courseOfRemovedToolIsNotSynced() {
        // Arrange.
        linkToLms(TestData.Users.MAIN_COURSE_STUDENT_ID, "6");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, "removed-tool", MAIN_COURSE_MEMBERS);
        // Запроси синхронизация этот список, пустой ответ отнял бы у студента роль.
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS);

        // Act.
        syncService.syncAll();

        // Assert.
        assertTrue(hasMainCourseRole(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemRole.STUDENT));
    }

    /** Курсы заблокированной LMS не синхронизируются: её список участников больше не решает, у кого какая роль. */
    @Test
    void courseOfBannedLmsIsNotSynced() {
        // Arrange.
        linkToLms(TestData.Users.MAIN_COURSE_STUDENT_ID, "6");
        membershipService.rememberSource(TestData.Courses.MAIN_ID, ISSUER, CLIENT_ID, MAIN_COURSE_MEMBERS);
        TestLtiMembershipProvider.hasMembers(MAIN_COURSE_MEMBERS);
        educationResourceRepository.updateTrustStatus(TestData.EducationResources.ID, EducationResourceTrustStatus.BANNED);

        // Act.
        syncService.syncAll();

        // Assert.
        assertTrue(hasMainCourseRole(TestData.Users.MAIN_COURSE_STUDENT_ID, SystemRole.STUDENT));
    }

    private void linkToLms(long userId, String lmsUserId) {
        educationResourceUserRepository.save(new EducationResourceUserEntity(
                userRepository.getReferenceById(userId),
                educationResourceRepository.getReferenceById(TestData.EducationResources.ID),
                lmsUserId));
    }

    private boolean hasMainCourseRole(long userId, Role role) {
        return authService.hasRole(userId, role, PermissionScope.course(TestData.Courses.MAIN_ID));
    }

    private static LtiCourseMemberData active(String lmsUserId, String... roles) {
        return new LtiCourseMemberData(lmsUserId, List.of(roles), true);
    }
}
