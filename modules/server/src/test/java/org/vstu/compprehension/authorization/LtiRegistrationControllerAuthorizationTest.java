package org.vstu.compprehension.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LtiRegistrationControllerAuthorizationTest extends AbstractAuthorizationTest {

    private static final String REGISTERED_LMS = "https://registered-lms.test.local";

    @Autowired private LtiRegistrationDataService ltiRegistrationService;

    /** Админ системы создаёт ссылку регистрации. */
    @Test
    void createInviteAllowedForAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations/invites"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                // Фронт разбирает срок действия как ISO-8601 строку.
                .andExpect(jsonPath("$.expiresAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")));
    }

    /** Админ образовательного ресурса LMS не подключает: это действие на всю систему. */
    @Test
    void createInviteForbiddenForEducationResourceAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.Users.EDUCATION_RESOURCE_ADMIN_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations/invites"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Список подключённых LMS виден админу системы. */
    @Test
    void getRegistrationsAllowedForAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(get("/api/lti/registrations"));

        // Assert.
        result.andExpect(status().isOk());
    }

    /** Преподавателю список подключённых LMS недоступен. */
    @Test
    void getRegistrationsForbiddenForTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/lti/registrations"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** С неизвестной ссылкой LMS получает страницу с отказом, а не JSON. */
    @Test
    void registerWithUnknownInviteShowsForbiddenPage() throws Exception {
        // Act.
        var result = mockMvc.perform(get("/lti/1_3/register/unknown-invite")
                .param("openid_configuration", "https://lms.test.local/mod/lti/openid-configuration.php"));

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Регистрация не удалась")));
    }

    /** Админ удаляет регистрацию: LMS больше не подключена, использованная ссылка остаётся использованной. */
    @Test
    void deleteRegistrationAllowedForAdmin() throws Exception {
        // Arrange.
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID);
        var registration = registerLms(invite.token());
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/lti/registrations/" + registration.id()));

        // Assert.
        result.andExpect(status().isOk());
        assertTrue(ltiRegistrationService.findByIssuer(REGISTERED_LMS).isEmpty());
        assertThrows(SecurityException.class, () -> ltiRegistrationService.ensureInviteUsable(invite.token()));
    }

    /** Удалённую LMS можно подключить снова новой ссылкой. */
    @Test
    void deletedRegistrationCanBeRegisteredAgain() throws Exception {
        // Arrange.
        var registration = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID).token());
        actingAs(TestData.Users.ADMIN_ID);
        mockMvc.perform(delete("/api/lti/registrations/" + registration.id())).andExpect(status().isOk());

        // Act.
        var again = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID).token());

        // Assert.
        assertEquals(REGISTERED_LMS, again.issuer());
    }

    /** Преподаватель регистрации не удаляет. */
    @Test
    void deleteRegistrationForbiddenForTeacher() throws Exception {
        // Arrange.
        var registration = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID).token());
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/lti/registrations/" + registration.id()));

        // Assert.
        result.andExpect(status().isForbidden());
        assertTrue(ltiRegistrationService.findByIssuer(REGISTERED_LMS).isPresent());
    }

    /** Несуществующая регистрация — 404. */
    @Test
    void deleteUnknownRegistrationIsNotFound() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/lti/registrations/999999"));

        // Assert.
        result.andExpect(status().isNotFound());
    }

    private LtiRegistrationData registerLms(String inviteToken) {
        return ltiRegistrationService.registerByInvite(inviteToken, new NewLtiRegistrationData(
                REGISTERED_LMS, EducationResourceType.MOODLE, REGISTERED_LMS, "registered-client", null,
                REGISTERED_LMS + "/mod/lti/auth.php", REGISTERED_LMS + "/mod/lti/token.php",
                REGISTERED_LMS + "/mod/lti/certs.php"));
    }
}
