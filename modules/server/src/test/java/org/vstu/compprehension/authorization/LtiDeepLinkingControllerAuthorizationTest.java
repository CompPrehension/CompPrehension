package org.vstu.compprehension.authorization;

import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.controllers.LtiDeepLinkingController.DeepLinkBuildRequest;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.CourseFrontendService;
import org.vstu.compprehension.frontend.EducationResourceFrontendService;
import org.vstu.compprehension.frontend.dto.course.CreateCourseDto;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.services.RoleAssignmentService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LtiDeepLinkingControllerAuthorizationTest extends AbstractAuthorizationTest {

    private static final DeepLinkBuildRequest BUILD_REQUEST =
            new DeepLinkBuildRequest(List.of(TestData.Exercises.INHERITED_ID));
    private static final String UNTRUSTED_LMS_URL = "https://moodle.untrusted.test";
    private static final String UNTRUSTED_EXTERNAL_COURSE_ID = "ext-untrusted-course";

    @Autowired private EducationResourceFrontendService educationResourceService;
    @Autowired private CourseFrontendService courseService;
    @Autowired private RoleAssignmentService roleAssignmentService;

    @AfterEach
    void resetLtiContext() {
        TestLtiContextProvider.reset();
    }

    /** Сборка активностей требует CREATE_LMS_ACTIVITY. */
    @Test
    void buildForbiddenForCourseStudent() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** У ассистента CREATE_LMS_ACTIVITY нет. */
    @Test
    void buildForbiddenForCourseAssistant() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_ASSISTANT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Права проверяются в курсе запуска, а не там, где они у пользователя есть. */
    @Test
    void buildForbiddenForTeacherOfAnotherCourse() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.OTHER_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Глобальные права членство в курсе не подменяют. */
    @Test
    void buildForbiddenForGlobalExerciseAuthor() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Список активностей закрыт тем же правом, что и сборка. */
    @Test
    void existingForbiddenForCourseStudent() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(get("/api/lti/deep-link/existing"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без deep-linking-сессии отказ идёт раньше проверки прав. */
    @Test
    void buildRejectedWithoutDeepLinkingSession() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isBadRequest());
    }

    /** Без LTI-контекста курс определить не из чего. */
    @Test
    void buildRejectedWithoutLtiContext() throws Exception {
        // Arrange.
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isBadRequest());
    }

    /** Админ недоверенной LMS активности не собирает. */
    @Test
    void buildForbiddenForAdminOfUntrustedLms() throws Exception {
        // Arrange.
        var resource = educationResourceService.getOrCreate(
                UNTRUSTED_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.UNTRUSTED);
        courseService.getOrCreate(new CreateCourseDto(resource.id(), UNTRUSTED_EXTERNAL_COURSE_ID, null));
        roleAssignmentService.reconcileRoleInEducationResource(
                TestData.Users.MAIN_COURSE_TEACHER_ID, resource.id(), SystemRole.EDUCATION_RESOURCE_ADMIN);
        TestLtiContextProvider.launchedFromLms(UNTRUSTED_LMS_URL, UNTRUSTED_EXTERNAL_COURSE_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(BUILD_REQUEST)));

        // Assert.
        result.andExpect(status().isForbidden());
    }
}
