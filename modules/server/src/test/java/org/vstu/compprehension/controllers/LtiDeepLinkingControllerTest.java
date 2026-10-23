package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.vstu.compprehension.authorization.AbstractAuthorizationTest;
import org.vstu.compprehension.authorization.TestLtiContextProvider;
import org.vstu.compprehension.controllers.LtiDeepLinkingController.DeepLinkSettingsLinkRequest;
import org.vstu.compprehension.infrastructure.TestData;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LtiDeepLinkingControllerTest extends AbstractAuthorizationTest {

    private static final String CONTENT_ITEMS_CLAIM = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";

    @AfterEach
    void resetLtiContext() {
        TestLtiContextProvider.reset();
    }

    /** Активность настройки запускается по адресу инструмента, страницу выбирает custom-параметр; оценок у неё нет. */
    @Test
    void buildSettingsLinkReturnsSingleSettingsActivity() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build-settings-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new DeepLinkSettingsLinkRequest("Настройка упражнений"))))
                .andExpect(status().isOk())
                .andReturn();

        // Assert.
        var jwt = new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("jwt").asText();
        List<Object> items = SignedJWT.parse(jwt).getJWTClaimsSet().getListClaim(CONTENT_ITEMS_CLAIM);
        assertEquals(1, items.size());
        var item = (Map<?, ?>) items.get(0);
        assertEquals("ltiResourceLink", item.get("type"));
        assertEquals("Настройка упражнений", item.get("title"));
        assertEquals(Map.of("compph_page", "exercise-settings"), item.get("custom"));
        assertFalse(item.containsKey("url"));
        assertFalse(item.containsKey("lineItem"));
    }

    /** Ответ выпущен «в прошлом»: Moodle не допускает iat позже своих часов даже на секунду. */
    @Test
    void buildSettingsLinkIssuesResponseBackdated() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);
        var requestedAt = Instant.now();

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build-settings-link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new DeepLinkSettingsLinkRequest("Настройка упражнений"))))
                .andExpect(status().isOk())
                .andReturn();

        // Assert.
        var jwt = new ObjectMapper().readTree(result.getResponse().getContentAsString()).get("jwt").asText();
        var claims = SignedJWT.parse(jwt).getJWTClaimsSet();
        assertTrue(claims.getIssueTime().toInstant().isBefore(requestedAt.minusSeconds(30)));
        assertTrue(claims.getExpirationTime().toInstant().isAfter(requestedAt));
    }

    /** Студент активность настройки не добавляет. */
    @Test
    void buildSettingsLinkForbiddenForCourseStudent() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build-settings-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new DeepLinkSettingsLinkRequest("Настройка упражнений"))));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Без названия активность не создаётся. */
    @Test
    void buildSettingsLinkRejectsBlankTitle() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestLtiContextProvider.withDeepLinkingSession();
        actingAs(TestData.Users.MAIN_COURSE_TEACHER_ID);

        // Act.
        var result = mockMvc.perform(post("/api/lti/deep-link/build-settings-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(new DeepLinkSettingsLinkRequest(" "))));

        // Assert.
        result.andExpect(status().isBadRequest());
    }
}
