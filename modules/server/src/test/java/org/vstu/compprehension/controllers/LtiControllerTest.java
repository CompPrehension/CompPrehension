package org.vstu.compprehension.controllers;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.vstu.compprehension.authorization.TestLtiContextProvider;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.EducationResourceFrontendService;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class LtiControllerTest extends AbstractIntegrationTest {

    private static final String NEW_EXTERNAL_COURSE_ID = "ext-course-new";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private EducationResourceRepository educationResourceRepository;
    @Autowired private EducationResourceFrontendService educationResourceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void resetLtiContext() {
        TestLtiContextProvider.reset();
    }

    /** Уже доверенная LMS пропускается без изменений. */
    @Test
    void launchFromKnownTrustedLmsRedirectsToCourse() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);

        // Act.
        var result = launchExerciseSettings();

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/exercise-settings?courseId=" + TestData.Courses.MAIN_ID));
    }

    /** LMS на поддомене доверенного хоста регистрируется доверенной. */
    @Test
    void launchFromNewLmsOnTrustedSubdomainCreatesTrustedResource() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromLms("https://moodle.trusted.test", NEW_EXTERNAL_COURSE_ID);

        // Act.
        var result = launchExerciseSettings();

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, notNullValue()));
        assertEquals(EducationResourceTrustStatus.TRUSTED, trustStatusOf("https://moodle.trusted.test"));
    }

    /** Сам доверенный хост тоже доверенный. */
    @Test
    void launchFromNewLmsOnTrustedHostCreatesTrustedResource() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromLms("https://trusted.test:8443", NEW_EXTERNAL_COURSE_ID);

        // Act.
        var result = launchExerciseSettings();

        // Assert.
        result.andExpect(status().is3xxRedirection());
        assertEquals(EducationResourceTrustStatus.TRUSTED, trustStatusOf("https://trusted.test:8443"));
    }

    /** Чужая LMS регистрируется недоверенной, запуск отклоняется, сессия не аутентифицируется. */
    @Test
    void launchFromNewUntrustedLmsIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromLms("https://moodle.untrusted.test", NEW_EXTERNAL_COURSE_ID);

        // Act.
        var result = launchExerciseSettings();

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, nullValue()));
        assertEquals(EducationResourceTrustStatus.UNTRUSTED, trustStatusOf("https://moodle.untrusted.test"));
    }

    /** Доверенный хост сравнивается по границе домена, а не по подстроке. */
    @Test
    void launchFromLookalikeHostIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromLms("https://eviltrusted.test", NEW_EXTERNAL_COURSE_ID);

        // Act.
        var result = launchExerciseSettings();

        // Assert.
        result.andExpect(status().isForbidden());
        assertEquals(EducationResourceTrustStatus.UNTRUSTED, trustStatusOf("https://eviltrusted.test"));
    }

    /** Статус уже существующей LMS не пересчитывается по доверенным хостам. */
    @Test
    void launchFromBannedLmsOnTrustedHostIsForbidden() throws Exception {
        // Arrange.
        var url = "https://banned.trusted.test";
        educationResourceService.getOrCreate(url, EducationResourceType.MOODLE, EducationResourceTrustStatus.BANNED);
        TestLtiContextProvider.launchedFromLms(url, NEW_EXTERNAL_COURSE_ID);

        // Act.
        var result = launchExerciseSettings();

        // Assert.
        result.andExpect(status().isForbidden());
        assertEquals(EducationResourceTrustStatus.BANNED, trustStatusOf(url));
    }

    private ResultActions launchExerciseSettings() throws Exception {
        return mockMvc.perform(post("/lti/1_3/exercise-settings").param("id_token", idToken()));
    }

    private static String idToken() {
        var now = Instant.now();
        return new PlainJWT(new JWTClaimsSet.Builder()
                .subject("lti-user")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                        List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"))
                .build()).serialize();
    }

    private EducationResourceTrustStatus trustStatusOf(String url) {
        return educationResourceRepository.findByUrlAndType(url, EducationResourceType.MOODLE)
                .map(EducationResourceEntity::getTrustStatus)
                .orElseThrow();
    }
}
