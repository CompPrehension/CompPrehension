package org.vstu.compprehension.adapters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestLtiContextProvider;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.data.UserDataRepository;
import org.vstu.compprehension.services.CourseDataService;
import org.vstu.compprehension.services.EducationResourceService;
import org.vstu.compprehension.services.LtiContextProvider;
import org.vstu.compprehension.services.RoleAssignmentService;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Transactional
class UserSignInTest extends AbstractIntegrationTest {

    private static final String OTHER_LMS_URL = "https://other-lms.test.local";
    private static final String IDP_ISSUER = "https://idp.test.local/realms/test";

    @Autowired private UserDataRepository users;
    @Autowired private EducationResourceService educationResourceService;
    @Autowired private LtiContextProvider ltiContextProvider;
    @Autowired private CourseDataService courseService;
    @Autowired private RoleAssignmentService roleAssignmentService;
    @Autowired private Mapper<UserAccountData, UserData> currentUserMapper;
    @Autowired private AuthFrontendService authService;

    private UserServiceImpl signIn;

    @BeforeEach
    void createSignIn() {
        signIn = new UserServiceImpl(users, educationResourceService, ltiContextProvider,
                courseService, roleAssignmentService, currentUserMapper);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
        TestLtiContextProvider.reset();
    }

    /** Одинаковый email в разных LMS не склеивает учётки: каждая LMS отвечает только за своих пользователей. */
    @Test
    void sameEmailFromTwoLmsGivesTwoUsers() {
        // Arrange.
        educationResourceService.getOrCreate(OTHER_LMS_URL, EducationResourceType.MOODLE, EducationResourceTrustStatus.TRUSTED);
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        long fromMainLms = signInFromLti("7", "teacher@vstu.ru").id();
        TestLtiContextProvider.launchedFromLms(OTHER_LMS_URL, "other-lms-course");

        // Act.
        long fromOtherLms = signInFromLti("7", "teacher@vstu.ru").id();

        // Assert.
        assertNotEquals(fromMainLms, fromOtherLms);
    }

    /** LMS не может выдать своего пользователя за админа системы, подставив его email. */
    @Test
    void ltiLaunchWithAdminEmailDoesNotGetAdminAccount() {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);

        // Act.
        long userId = signInFromLti("8", "global-admin@test.local").id();

        // Assert.
        assertNotEquals(TestData.Users.ADMIN_ID, userId);
        assertFalse(authService.canRegisterLms(userId));
    }

    /** Повторный запуск из той же LMS находит ту же учётку, даже если в LMS сменили email. */
    @Test
    void repeatedLtiLaunchFindsSameUserWithNewEmail() {
        // Arrange.
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);
        long firstLaunch = signInFromLti("9", "old@vstu.ru").id();

        // Act.
        var secondLaunch = signInFromLti("9", "new@vstu.ru");

        // Assert.
        assertEquals(firstLaunch, secondLaunch.id());
        assertEquals("new@vstu.ru", secondLaunch.email());
    }

    /** Вход на сайт и запуск из LMS с тем же email — разные учётки. */
    @Test
    void idpAndLtiWithSameEmailAreDifferentUsers() {
        // Arrange.
        long fromIdp = signInFromIdp(IDP_ISSUER, "idp-subject", "teacher@vstu.ru").id();
        TestLtiContextProvider.launchedFromCourse(TestData.Courses.MAIN_EXTERNAL_ID);

        // Act.
        long fromLti = signInFromLti("10", "teacher@vstu.ru").id();

        // Assert.
        assertNotEquals(fromIdp, fromLti);
    }

    /** Повторный вход на сайт находит ту же учётку, даже если сменился email. */
    @Test
    void repeatedIdpSignInFindsSameUser() {
        // Arrange.
        long firstSignIn = signInFromIdp(IDP_ISSUER, "idp-subject", "old@vstu.ru").id();

        // Act.
        var secondSignIn = signInFromIdp(IDP_ISSUER, "idp-subject", "new@vstu.ru");

        // Assert.
        assertEquals(firstSignIn, secondSignIn.id());
        assertEquals("new@vstu.ru", secondSignIn.email());
    }

    private UserData signInFromLti(String subject, String email) {
        return signInWith(OidcIdToken.withTokenValue("lti-token")
                .subject(subject)
                .claim("email", email)
                .claim("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build(), "ROLE_Learner");
    }

    /** Id пользователя уникален только в пределах провайдера: другой провайдер с тем же id — другая учётка. */
    @Test
    void sameSubjectFromAnotherIdpGivesAnotherUser() {
        // Arrange.
        long fromFirstIdp = signInFromIdp(IDP_ISSUER, "idp-subject", "teacher@vstu.ru").id();

        // Act.
        long fromSecondIdp = signInFromIdp("https://idp.test.local/realms/other", "idp-subject", "teacher@vstu.ru").id();

        // Assert.
        assertNotEquals(fromFirstIdp, fromSecondIdp);
    }

    private UserData signInFromIdp(String issuer, String subject, String email) {
        return signInWith(OidcIdToken.withTokenValue("idp-token")
                .issuer(issuer)
                .subject(subject)
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build(), "ROLE_Student");
    }

    private UserData signInWith(OidcIdToken token, String... roles) {
        var authorities = Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        var user = new DefaultOidcUser(authorities, token);
        SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(user, authorities, "test"));
        return signIn.getCurrentUser();
    }
}
