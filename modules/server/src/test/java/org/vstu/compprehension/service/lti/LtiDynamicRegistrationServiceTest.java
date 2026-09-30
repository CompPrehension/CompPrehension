package org.vstu.compprehension.service.lti;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.adapters.lti.LtiMembershipClient;
import org.vstu.compprehension.adapters.lti.LtiToolConfigurationService;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.entities.external_system.LtiRegistrationInviteEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.LtiRegistrationInviteRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Transactional
class LtiDynamicRegistrationServiceTest extends AbstractIntegrationTest {

    private static final String LMS_ISSUER = "https://new-lms.test.local";
    private static final String OPENID_CONFIGURATION_URL = LMS_ISSUER + "/mod/lti/openid-configuration.php";
    private static final String REGISTRATION_ENDPOINT = LMS_ISSUER + "/mod/lti/openid-registration.php";
    private static final String REGISTRATION_TOKEN = "lms-registration-token";
    // Публичный адрес инструмента из application-test.properties.
    private static final String TOOL_BASE_URL = "https://tool.test";

    private static final String PLATFORM_CONFIGURATION = """
            {
              "issuer": "https://new-lms.test.local",
              "authorization_endpoint": "https://new-lms.test.local/mod/lti/auth.php",
              "token_endpoint": "https://new-lms.test.local/mod/lti/token.php",
              "jwks_uri": "https://new-lms.test.local/mod/lti/certs.php",
              "registration_endpoint": "https://new-lms.test.local/mod/lti/openid-registration.php",
              "https://purl.imsglobal.org/spec/lti-platform-configuration": {"product_family_code": "moodle"}
            }
            """;
    private static final String TOOL_REGISTRATION_RESPONSE = """
            {
              "client_id": "dynamic-client",
              "https://purl.imsglobal.org/spec/lti-tool-configuration": {"deployment_id": "7"}
            }
            """;

    @Autowired private LtiToolConfigurationService toolConfiguration;
    @Autowired private LtiRegistrationDataService ltiRegistrationService;
    @Autowired private LtiRegistrationRegistry ltiRegistrationRegistry;
    @Autowired private ExternalSystemDataRepository externalSystems;
    @Autowired private EducationResourceRepository educationResourceRepository;
    @Autowired private LtiRegistrationInviteRepository inviteRepository;
    @Autowired private UserRepository userRepository;

    private MockRestServiceServer lms;
    private LtiDynamicRegistrationService service;

    @BeforeEach
    void setUpFakeLms() {
        // Свой RestTemplate: общий бин из контекста подменять нельзя, контекст переиспользуется другими тестами.
        var restTemplate = new RestTemplate();
        lms = MockRestServiceServer.bindTo(restTemplate).build();
        service = new LtiDynamicRegistrationService(restTemplate, toolConfiguration, ltiRegistrationService);
    }

    /**
     * Регистрация сообщает LMS наши адреса и нужные службы (оценки, участники курса), сохраняет выданный client_id
     * и описание из ссылки, делает LMS доверенной и гасит ссылку.
     */
    @Test
    void registerCreatesTrustedRegistrationAndUsesInvite() {
        // Arrange.
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, "Сайт Moodle");
        expectPlatformConfiguration(PLATFORM_CONFIGURATION);
        lms.expect(requestTo(REGISTRATION_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + REGISTRATION_TOKEN))
                .andExpect(jsonPath("$.initiate_login_uri").value(TOOL_BASE_URL + "/lti/login"))
                .andExpect(jsonPath("$.redirect_uris[0]").value(TOOL_BASE_URL + "/lti/launch"))
                .andExpect(jsonPath("$.jwks_uri").value(TOOL_BASE_URL + "/lti/jwks"))
                .andExpect(jsonPath("$.scope").value(containsString(LtiMembershipClient.SCOPE)))
                .andExpect(jsonPath("$['https://purl.imsglobal.org/spec/lti-tool-configuration'].messages[0].type")
                        .value("LtiDeepLinkingRequest"))
                .andRespond(withSuccess(TOOL_REGISTRATION_RESPONSE, MediaType.APPLICATION_JSON));

        // Act.
        var registration = service.register(invite.token(), OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN);

        // Assert.
        lms.verify();
        assertEquals(LMS_ISSUER, registration.issuer());
        assertEquals("dynamic-client", registration.clientId());
        assertEquals("7", registration.deploymentId());
        assertEquals("Сайт Moodle", registration.description());
        assertEquals(EducationResourceTrustStatus.TRUSTED, trustStatusOf(LMS_ISSUER));
        assertThrows(SecurityException.class, () -> ltiRegistrationService.ensureInviteUsable(invite.token()));

        var platform = ltiRegistrationRegistry.requireByIssuerAndClientId(LMS_ISSUER, "dynamic-client");
        assertEquals(LMS_ISSUER + "/mod/lti/auth.php", platform.authorizationEndpoint());
        assertEquals(new LtiPlatformKeyData.Jwks(LMS_ISSUER + "/mod/lti/certs.php"), platform.platformKey());
    }

    /** Без годной ссылки сервер не ходит в LMS вовсе. */
    @Test
    void registerWithUnknownInviteIsForbiddenBeforeAnyRequest() {
        // Act & Assert.
        assertThrows(SecurityException.class,
                () -> service.register("unknown-invite", OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN));
        lms.verify();
    }

    /** Просроченная ссылка не принимается. */
    @Test
    void registerWithExpiredInviteIsForbidden() throws Exception {
        // Arrange.
        var invite = new LtiRegistrationInviteEntity();
        invite.setTokenHash(sha256Hex("expired-invite"));
        invite.setCreatedBy(userRepository.getReferenceById(TestData.Users.ADMIN_ID));
        invite.setCreatedAt(Instant.now().minusSeconds(3 * 24 * 3600));
        invite.setExpiresAt(Instant.now().minusSeconds(24 * 3600));
        inviteRepository.save(invite);

        // Act & Assert.
        assertThrows(SecurityException.class,
                () -> service.register("expired-invite", OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN));
        lms.verify();
    }

    /** Ссылка одноразовая. */
    @Test
    void registerWithUsedInviteIsForbidden() {
        // Arrange.
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null);
        expectPlatformConfiguration(PLATFORM_CONFIGURATION);
        lms.expect(requestTo(REGISTRATION_ENDPOINT))
                .andRespond(withSuccess(TOOL_REGISTRATION_RESPONSE, MediaType.APPLICATION_JSON));
        service.register(invite.token(), OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN);

        // Act & Assert.
        assertThrows(SecurityException.class,
                () -> service.register(invite.token(), OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN));
        lms.verify();
    }

    /** Настройки, выданные не с адреса самой LMS, отклоняются до регистрации. */
    @Test
    void registerWithConfigurationFromForeignHostIsForbidden() {
        // Arrange.
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null);
        var foreignConfigurationUrl = "https://new-lms.test.local.evil.test/openid-configuration";
        lms.expect(requestTo(foreignConfigurationUrl))
                .andRespond(withSuccess(PLATFORM_CONFIGURATION, MediaType.APPLICATION_JSON));

        // Act & Assert.
        assertThrows(SecurityException.class,
                () -> service.register(invite.token(), foreignConfigurationUrl, REGISTRATION_TOKEN));
        lms.verify();
        ltiRegistrationService.ensureInviteUsable(invite.token());
    }

    /** Уже подключённой LMS ссылка добавляет ещё один инструмент: у каждого свой client_id, прежний продолжает работать. */
    @Test
    void registerOfAlreadyRegisteredLmsAddsAnotherTool() {
        // Arrange.
        // https://lms.test.local с client_id test-client зарегистрирована в data.sql.
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null);
        var lmsUrl = TestData.EducationResources.URL;
        lms.expect(requestTo(lmsUrl + "/mod/lti/openid-configuration.php"))
                .andRespond(withSuccess(PLATFORM_CONFIGURATION.replace(LMS_ISSUER, lmsUrl), MediaType.APPLICATION_JSON));
        lms.expect(requestTo(lmsUrl + "/mod/lti/openid-registration.php"))
                .andRespond(withSuccess(TOOL_REGISTRATION_RESPONSE, MediaType.APPLICATION_JSON));

        // Act.
        service.register(invite.token(), lmsUrl + "/mod/lti/openid-configuration.php", REGISTRATION_TOKEN);

        // Assert.
        lms.verify();
        assertEquals(Set.of("test-client", "dynamic-client"), ltiRegistrationService.findAllByIssuer(lmsUrl).stream()
                .map(LtiRegistrationData::clientId).collect(Collectors.toSet()));
    }

    /** Заблокированную LMS ссылка не разблокирует: регистрация не сохраняется. */
    @Test
    void registerOfBannedLmsIsForbidden() {
        // Arrange.
        externalSystems.createEducationResourceIfAbsent(LMS_ISSUER, EducationResourceTrustStatus.BANNED);
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null);
        expectPlatformConfiguration(PLATFORM_CONFIGURATION);
        lms.expect(requestTo(REGISTRATION_ENDPOINT))
                .andRespond(withSuccess(TOOL_REGISTRATION_RESPONSE, MediaType.APPLICATION_JSON));

        // Act & Assert.
        assertThrows(SecurityException.class,
                () -> service.register(invite.token(), OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN));
        assertEquals(EducationResourceTrustStatus.BANNED, trustStatusOf(LMS_ISSUER));
        assertTrue(ltiRegistrationService.findAllByIssuer(LMS_ISSUER).isEmpty());
        ltiRegistrationService.ensureInviteUsable(invite.token());
    }

    /** Ранее недоверенная LMS после регистрации по ссылке становится доверенной. */
    @Test
    void registerOfUntrustedLmsMakesItTrusted() {
        // Arrange.
        externalSystems.createEducationResourceIfAbsent(LMS_ISSUER, EducationResourceTrustStatus.UNTRUSTED);
        var invite = ltiRegistrationService.createInvite(TestData.Users.ADMIN_ID, null);
        expectPlatformConfiguration(PLATFORM_CONFIGURATION);
        lms.expect(requestTo(REGISTRATION_ENDPOINT))
                .andRespond(withSuccess(TOOL_REGISTRATION_RESPONSE, MediaType.APPLICATION_JSON));

        // Act.
        service.register(invite.token(), OPENID_CONFIGURATION_URL, REGISTRATION_TOKEN);

        // Assert.
        assertEquals(EducationResourceTrustStatus.TRUSTED, trustStatusOf(LMS_ISSUER));
        assertEquals(1, ltiRegistrationService.findAllByIssuer(LMS_ISSUER).size());
    }

    private void expectPlatformConfiguration(String configuration) {
        lms.expect(requestTo(OPENID_CONFIGURATION_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(configuration, MediaType.APPLICATION_JSON));
    }

    private EducationResourceTrustStatus trustStatusOf(String url) {
        var resource = educationResourceRepository.findByUrl(url);
        assertTrue(resource.isPresent());
        return resource.get().getTrustStatus();
    }

    private static String sha256Hex(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
