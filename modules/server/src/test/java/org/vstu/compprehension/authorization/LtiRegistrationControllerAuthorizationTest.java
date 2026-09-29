package org.vstu.compprehension.authorization;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.vstu.compprehension.common.RsaKeyHelper;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        var result = mockMvc.perform(post("/api/lti/registrations/invites")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"Сайт Moodle\"}"));

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
        var result = mockMvc.perform(post("/api/lti/registrations/invites")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"Сайт Moodle\"}"));

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

    /** Админ подключает инструмент, заведённый в LMS вручную, например курсовой инструмент Moodle, с описанием. */
    @Test
    void registerManuallyAllowedForAdmin() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson(REGISTERED_LMS, "course-tool")));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("course-tool"))
                .andExpect(jsonPath("$.description").value("Курс ОАиП"))
                .andExpect(jsonPath("$.method").value("MANUAL"));
        assertTrue(ltiRegistrationService.findByIssuerAndClientId(REGISTERED_LMS, "course-tool").isPresent());
    }

    /** Две LMS в подкаталогах одного хоста — разные LMS: пользователи с одинаковым id в них не смешиваются. */
    @Test
    void lmsInSubdirectoriesOfOneHostAreDifferentLms() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);
        mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson("https://campus.test.local/moodle-a", "tool-a")))
                .andExpect(status().isOk());

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson("https://campus.test.local/moodle-b", "tool-b")));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.lmsUrl").value("https://campus.test.local/moodle-b"));
        assertNotEquals(
                ltiRegistrationService.findByIssuerAndClientId("https://campus.test.local/moodle-a", "tool-a").orElseThrow().educationResourceId(),
                ltiRegistrationService.findByIssuerAndClientId("https://campus.test.local/moodle-b", "tool-b").orElseThrow().educationResourceId());
    }

    /** LMS без JWKS подключается по её открытому ключу, вставленному в PEM. */
    @Test
    void registerManuallyWithPlatformPublicKey() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);
        var lmsKey = (RSAPublicKey) KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson(REGISTERED_LMS, "key-tool", "PUBLIC_KEY", RsaKeyHelper.toPem(lmsKey))));

        // Assert.
        result.andExpect(status().isOk());
        assertEquals(new LtiPlatformKeyData.PublicKey(Base64.getEncoder().encodeToString(lmsKey.getEncoded())),
                ltiRegistrationService.findByIssuerAndClientId(REGISTERED_LMS, "key-tool").orElseThrow().platformKey());
    }

    /** Вместо открытого ключа LMS вставлено не то — инструмент не подключается. */
    @Test
    void registerManuallyWithMalformedPlatformPublicKeyIsBadRequest() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson(REGISTERED_LMS, "key-tool", "PUBLIC_KEY", "not a key")));

        // Assert.
        result.andExpect(status().isBadRequest());
        assertTrue(ltiRegistrationService.findByIssuerAndClientId(REGISTERED_LMS, "key-tool").isEmpty());
    }

    /** Подсказка для ручного подключения даёт адреса инструмента и открытый ключ, парный опубликованному в JWKS. */
    @Test
    void toolConfigurationShowsAddressesAndPublishedKey() throws Exception {
        // Arrange.
        actingAs(TestData.Users.ADMIN_ID);
        var jwks = JWKSet.parse(mockMvc.perform(get("/lti/jwks")).andReturn().getResponse().getContentAsString());

        // Act.
        var result = mockMvc.perform(get("/api/lti/tool-configuration"));

        // Assert.
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.launchUrl").value("https://tool.test/lti/launch"))
                .andExpect(jsonPath("$.loginUrl").value("https://tool.test/lti/login"))
                .andExpect(jsonPath("$.jwksUrl").value("https://tool.test/lti/jwks"));
        var pem = JsonPath.<String>read(result.andReturn().getResponse().getContentAsString(), "$.publicKeyPem");
        assertEquals(jwks.getKeys().getFirst().toRSAKey().toRSAPublicKey(), RsaKeyHelper.parsePublicKey(pem));
    }

    /** Преподавателю подсказка для подключения LMS недоступна. */
    @Test
    void toolConfigurationForbiddenForTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(get("/api/lti/tool-configuration"));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Тот же инструмент той же LMS второй раз не подключается. */
    @Test
    void registerManuallyRegisteredToolIsConflict() throws Exception {
        // Arrange.
        // https://lms.test.local с client_id test-client зарегистрирована в data.sql.
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson(TestData.EducationResources.URL, "test-client")));

        // Assert.
        result.andExpect(status().isConflict());
    }

    /** Преподаватель LMS не подключает: подключение решает, чьим подписям верить. */
    @Test
    void registerManuallyForbiddenForTeacher() throws Exception {
        // Arrange.
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(manualRegistrationJson(REGISTERED_LMS, "course-tool")));

        // Assert.
        result.andExpect(status().isForbidden());
        assertTrue(ltiRegistrationService.findAllByIssuer(REGISTERED_LMS).isEmpty());
    }

    /** С неизвестной ссылкой LMS получает страницу с отказом, а не JSON. */
    @Test
    void registerWithUnknownInviteShowsForbiddenPage() throws Exception {
        // Act.
        var result = mockMvc.perform(get("/lti/register/unknown-invite")
                .param("openid_configuration", "https://lms.test.local/mod/lti/openid-configuration.php"));

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Регистрация не удалась")));
    }

    /** Админ удаляет регистрацию: LMS больше не подключена, использованная ссылка остаётся использованной. */
    @Test
    void deleteRegistrationAllowedForAdmin() throws Exception {
        // Arrange.
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null);
        var registration = registerLms(invite.token());
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/lti/registrations/" + registration.id()));

        // Assert.
        result.andExpect(status().isOk());
        assertTrue(ltiRegistrationService.findAllByIssuer(REGISTERED_LMS).isEmpty());
        assertThrows(SecurityException.class, () -> ltiRegistrationService.ensureInviteUsable(invite.token()));
    }

    /** Удалённую LMS можно подключить снова новой ссылкой. */
    @Test
    void deletedRegistrationCanBeRegisteredAgain() throws Exception {
        // Arrange.
        var registration = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null).token());
        actingAs(TestData.Users.ADMIN_ID);
        mockMvc.perform(delete("/api/lti/registrations/" + registration.id())).andExpect(status().isOk());

        // Act.
        var again = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null).token());

        // Assert.
        assertEquals(REGISTERED_LMS, again.issuer());
    }

    /** Преподаватель регистрации не удаляет. */
    @Test
    void deleteRegistrationForbiddenForTeacher() throws Exception {
        // Arrange.
        var registration = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null).token());
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(delete("/api/lti/registrations/" + registration.id()));

        // Assert.
        result.andExpect(status().isForbidden());
        assertTrue(!ltiRegistrationService.findAllByIssuer(REGISTERED_LMS).isEmpty());
    }

    /** Админ меняет описание инструмента, чтобы различать несколько инструментов одной LMS. */
    @Test
    void updateDescriptionAllowedForAdmin() throws Exception {
        // Arrange.
        var registration = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null).token());
        actingAs(TestData.Users.ADMIN_ID);

        // Act.
        var result = mockMvc.perform(put("/api/lti/registrations/" + registration.id() + "/description")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"  Курс ОАиП  \"}"));

        // Assert.
        result.andExpect(status().isOk());
        assertEquals("Курс ОАиП", ltiRegistrationService.findByIssuerAndClientId(REGISTERED_LMS, "registered-client")
                .orElseThrow().description());
    }

    /** Преподаватель описание инструмента не меняет. */
    @Test
    void updateDescriptionForbiddenForTeacher() throws Exception {
        // Arrange.
        var registration = registerLms(ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null).token());
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(put("/api/lti/registrations/" + registration.id() + "/description")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\": \"Курс ОАиП\"}"));

        // Assert.
        result.andExpect(status().isForbidden());
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

    private static String manualRegistrationJson(String issuer, String clientId) {
        return manualRegistrationJson(issuer, clientId, "JWKS", issuer + "/mod/lti/certs.php");
    }

    private static String manualRegistrationJson(String issuer, String clientId, String platformKeyType, String platformKey) {
        return """
                {"issuer": "%1$s", "clientId": "%2$s", "description": "Курс ОАиП", "deploymentId": "",
                 "authorizationEndpoint": "%1$s/mod/lti/auth.php", "tokenEndpoint": "%1$s/mod/lti/token.php",
                 "platformKeyType": "%3$s", "platformKey": "%4$s"}
                """.formatted(issuer, clientId, platformKeyType, platformKey.replace("\n", "\\n"));
    }

    private LtiRegistrationData registerLms(String inviteToken) {
        return ltiRegistrationService.registerByInvite(inviteToken, new NewLtiRegistrationData(
                REGISTERED_LMS, EducationResourceType.MOODLE, REGISTERED_LMS, "registered-client", null,
                REGISTERED_LMS + "/mod/lti/auth.php", REGISTERED_LMS + "/mod/lti/token.php",
                new LtiPlatformKeyData.Jwks(REGISTERED_LMS + "/mod/lti/certs.php")));
    }
}
