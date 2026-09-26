package org.vstu.compprehension.controllers;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;
import org.vstu.compprehension.adapters.LtiContextHolder;
import org.vstu.compprehension.authorization.TestLtiContextProvider;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.CourseFrontendService;
import org.vstu.compprehension.frontend.EducationResourceFrontendService;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class LtiControllerTest extends AbstractIntegrationTest {

    // Регистрация LMS из application-test.properties.
    private static final String REGISTERED_ISSUER = "https://lms.test.local";
    private static final String REGISTERED_CLIENT_ID = "test-client";

    private static final String MESSAGE_TYPE_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    private static final String CUSTOM_CLAIM = "https://purl.imsglobal.org/spec/lti/claim/custom";

    private static final String NEW_LMS_URL = "https://new-lms.test.local";
    private static final String NEW_EXTERNAL_COURSE_ID = "ext-course-new";

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private EducationResourceRepository educationResourceRepository;
    @Autowired private EducationResourceFrontendService educationResourceService;
    @Autowired private CourseFrontendService courseService;

    @Value("${test.lti.platform-private-key-pkcs8-base64}")
    private String platformPrivateKeyBase64;

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
        TestUserService.reset();
    }

    // ---- login ----

    /** Логин зарегистрированной LMS уводит на её авторизацию с нашими state и nonce. */
    @Test
    void loginRedirectsToRegisteredLms() throws Exception {
        // Act.
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Assert.
        assertTrue(login.redirectUrl().startsWith(REGISTERED_ISSUER + "/mod/lti/auth.php?"));
        assertNotNull(login.state());
        assertNotNull(login.nonce());
    }

    /** Логин незарегистрированной LMS отклоняется, а не становится открытым редиректом. */
    @Test
    void loginFromUnregisteredIssuerIsForbidden() throws Exception {
        // Act.
        var result = performLogin("https://evil.test", REGISTERED_CLIENT_ID);

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Логин с чужим client_id отклоняется. */
    @Test
    void loginWithForeignClientIdIsForbidden() throws Exception {
        // Act.
        var result = performLogin(REGISTERED_ISSUER, "foreign-client");

        // Assert.
        result.andExpect(status().isForbidden());
    }

    // ---- JWKS ----

    /** JWKS публикует ключи регистраций из env и общий ключ инструмента для динамических регистраций. */
    @Test
    void jwksPublishesConfiguredAndSharedToolKeys() throws Exception {
        // Act.
        var result = mockMvc.perform(get("/lti/1_3/jwks"));

        // Assert.
        // Имена ключей: регистрация "test" из application-test.properties и общий ключ "tool".
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[*].kid", containsInAnyOrder("test", "tool")));
    }

    // ---- доверие к LMS ----

    /** Уже доверенная LMS пропускается без изменений. */
    @Test
    void launchFromKnownTrustedLmsRedirectsToCourse() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/exercise-settings?courseId=" + TestData.Courses.MAIN_ID))
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, notNullValue()));
    }

    /** Запуск вне курса курс не создаёт. */
    @Test
    void launchWithoutCourseDoesNotCreateCourse() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromLms(TestData.EducationResources.URL, null);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/exercise-settings?courseId=null"));
        assertEquals(2, courseService.getUserCourses(TestData.Users.ADMIN_ID).size());
    }

    /** Первый подписанный запуск новой LMS регистрирует её доверенной. */
    @Test
    void launchFromNewLmsCreatesTrustedResource() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromLms(NEW_LMS_URL, NEW_EXTERNAL_COURSE_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, notNullValue()));
        assertEquals(EducationResourceTrustStatus.TRUSTED, trustStatusOf(NEW_LMS_URL));
    }

    /** Недоверенная LMS остаётся закрытой, сессия не аутентифицируется. */
    @Test
    void launchFromUntrustedLmsIsForbidden() throws Exception {
        // Arrange.
        educationResourceService.getOrCreate(NEW_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.UNTRUSTED);
        TestLtiContextProvider.launchedFromLms(NEW_LMS_URL, NEW_EXTERNAL_COURSE_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, nullValue()));
        assertEquals(EducationResourceTrustStatus.UNTRUSTED, trustStatusOf(NEW_LMS_URL));
    }

    /** После отказа недоверенной LMS в сессии не остаётся её LTI-контекста. */
    @Test
    void launchFromUntrustedLmsClearsSessionLtiContext() throws Exception {
        // Arrange.
        educationResourceService.getOrCreate(NEW_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.UNTRUSTED);
        TestLtiContextProvider.launchedFromLms(NEW_LMS_URL, NEW_EXTERNAL_COURSE_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), platformPrivateKey()))
                .andReturn();

        // Assert.
        // Контроллер берёт контекст из TestLtiContextProvider, а сессионный LtiContextHolder заполняется из id_token.
        var holder = (LtiContextHolder) result.getRequest().getSession().getAttribute("scopedTarget.ltiContextHolder");
        assertEquals(Optional.empty(), holder.getCurrentLtiContext());
        assertEquals(Optional.empty(), holder.getCurrentDeepLinkingContext());
    }

    /** Заблокированная LMS остаётся заблокированной. */
    @Test
    void launchFromBannedLmsIsForbidden() throws Exception {
        // Arrange.
        educationResourceService.getOrCreate(NEW_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.BANNED);
        TestLtiContextProvider.launchedFromLms(NEW_LMS_URL, NEW_EXTERNAL_COURSE_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden());
        assertEquals(EducationResourceTrustStatus.BANNED, trustStatusOf(NEW_LMS_URL));
    }

    // ---- маршрутизация запуска ----

    /** Запуск без особых параметров открывает упражнение. */
    @Test
    void launchOpensExercise() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = mockMvc.perform(post("/lti/1_3/launch")
                .session(login.session())
                .param("id", String.valueOf(TestData.Exercises.MAIN_COURSE_ID))
                .param("id_token", sign(validClaims(login.nonce()).build(), platformPrivateKey()))
                .param("state", login.state()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/exercise?exerciseId=" + TestData.Exercises.MAIN_COURSE_ID
                        + "&courseId=" + TestData.Courses.MAIN_ID));
    }

    /** Custom-параметр страницы открывает настройку упражнений курса. */
    @Test
    void launchWithSettingsPageOpensExerciseSettings() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var claims = validClaims(login.nonce())
                .claim(CUSTOM_CLAIM, Map.of("compph_page", "exercise-settings"))
                .build();

        // Act.
        var result = launch("/lti/1_3/launch", login, sign(claims, platformPrivateKey()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/exercise-settings?courseId=" + TestData.Courses.MAIN_ID));
    }

    /** Старый адрес упражнения тоже понимает custom-параметр страницы: Tool URL старых инструментов — /exercise. */
    @Test
    void legacyExerciseLaunchWithSettingsPageOpensExerciseSettings() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var claims = validClaims(login.nonce())
                .claim(CUSTOM_CLAIM, Map.of("compph_page", "exercise-settings"))
                .build();

        // Act.
        var result = launch("/lti/1_3/exercise", login, sign(claims, platformPrivateKey()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/exercise-settings?courseId=" + TestData.Courses.MAIN_ID));
    }

    /** Запрос deep linking открывает выбор контента курса. */
    @Test
    void launchWithDeepLinkingRequestOpensCoursePicker() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        TestUserService.actAs(TestData.Users.MAIN_COURSE_TEACHER_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var claims = validClaims(login.nonce())
                .claim(MESSAGE_TYPE_CLAIM, "LtiDeepLinkingRequest")
                .build();

        // Act.
        var result = launch("/lti/1_3/launch", login, sign(claims, platformPrivateKey()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/course?courseId=" + TestData.Courses.MAIN_ID + "&lti=deeplink"));
    }

    // ---- проверка id_token ----

    /** Неподписанный токен отклоняется. */
    @Test
    void launchWithUnsignedTokenIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var idToken = new PlainJWT(validClaims(login.nonce()).build()).serialize();

        // Act.
        var result = launchExerciseSettings(login, idToken);

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, nullValue()));
    }

    /** Токен, подписанный не ключом LMS, отклоняется. */
    @Test
    void launchWithTokenSignedByForeignKeyIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var foreignKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims(login.nonce()).build(), foreignKey));

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, nullValue()));
    }

    /** Токен, выданный другому инструменту, отклоняется. */
    @Test
    void launchWithTokenForForeignClientIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var claims = validClaims(login.nonce()).audience("foreign-client").build();

        // Act.
        var result = launchExerciseSettings(login, sign(claims, platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Токен незарегистрированной LMS отклоняется. */
    @Test
    void launchWithTokenFromUnregisteredIssuerIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var claims = validClaims(login.nonce()).issuer("https://evil.test").build();

        // Act.
        var result = launchExerciseSettings(login, sign(claims, platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Просроченный токен отклоняется. */
    @Test
    void launchWithExpiredTokenIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var tenMinutesAgo = Instant.now().minusSeconds(600);
        var claims = validClaims(login.nonce())
                .issueTime(Date.from(tenMinutesAgo.minusSeconds(300)))
                .expirationTime(Date.from(tenMinutesAgo))
                .build();

        // Act.
        var result = launchExerciseSettings(login, sign(claims, platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    // ---- state и nonce ----

    /** Запуск без логина через наш /login отклоняется. */
    @Test
    void launchWithUnknownStateIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var foreignLogin = new Login(login.session(), login.redirectUrl(), "unknown-state", login.nonce());

        // Act.
        var result = launchExerciseSettings(foreignLogin, sign(validClaims(login.nonce()).build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden())
                .andExpect(request().sessionAttribute(SPRING_SECURITY_CONTEXT_KEY, nullValue()));
    }

    /** Токен с чужим nonce отклоняется. */
    @Test
    void launchWithForeignNonceIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);

        // Act.
        var result = launchExerciseSettings(login, sign(validClaims("foreign-nonce").build(), platformPrivateKey()));

        // Assert.
        result.andExpect(status().isForbidden());
    }

    /** Один логин пропускает только один запуск: перехваченный токен повторно не принимается. */
    @Test
    void launchReplayIsForbidden() throws Exception {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        var login = startLogin(REGISTERED_ISSUER, REGISTERED_CLIENT_ID);
        var idToken = sign(validClaims(login.nonce()).build(), platformPrivateKey());
        launchExerciseSettings(login, idToken).andExpect(status().is3xxRedirection());

        // Act.
        var result = launchExerciseSettings(login, idToken);

        // Assert.
        result.andExpect(status().isForbidden());
    }

    private record Login(MockHttpSession session, String redirectUrl, String state, String nonce) {
    }

    private Login startLogin(String issuer, String clientId) throws Exception {
        var result = performLogin(issuer, clientId).andExpect(status().is3xxRedirection()).andReturn();
        var redirectUrl = result.getResponse().getRedirectedUrl();
        var query = UriComponentsBuilder.fromUriString(redirectUrl).build().getQueryParams();
        // Логин начинает новую сессию, запуск должен прийти в неё.
        var session = (MockHttpSession) result.getRequest().getSession(false);
        return new Login(session, redirectUrl, query.getFirst("state"), query.getFirst("nonce"));
    }

    private ResultActions performLogin(String issuer, String clientId) throws Exception {
        return mockMvc.perform(post("/lti/1_3/login")
                .param("iss", issuer)
                .param("client_id", clientId)
                .param("login_hint", "lti-user")
                .param("lti_message_hint", "message-hint")
                .param("target_link_uri", "https://tool.test/lti/1_3/exercise-settings"));
    }

    private ResultActions launchExerciseSettings(Login login, String idToken) throws Exception {
        return launch("/lti/1_3/exercise-settings", login, idToken);
    }

    private ResultActions launch(String path, Login login, String idToken) throws Exception {
        return mockMvc.perform(post(path)
                .session(login.session())
                .param("id_token", idToken)
                .param("state", login.state()));
    }

    private static JWTClaimsSet.Builder validClaims(String nonce) {
        var now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(REGISTERED_ISSUER)
                .audience(REGISTERED_CLIENT_ID)
                .subject("lti-user")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("nonce", nonce)
                .claim("https://purl.imsglobal.org/spec/lti/claim/roles",
                        List.of("http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor"));
    }

    private static String sign(JWTClaimsSet claims, PrivateKey key) throws Exception {
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private PrivateKey platformPrivateKey() throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(platformPrivateKeyBase64)));
    }

    private EducationResourceTrustStatus trustStatusOf(String url) {
        return educationResourceRepository.findByUrlAndType(url, EducationResourceType.MOODLE)
                .map(EducationResourceEntity::getTrustStatus)
                .orElseThrow();
    }
}
