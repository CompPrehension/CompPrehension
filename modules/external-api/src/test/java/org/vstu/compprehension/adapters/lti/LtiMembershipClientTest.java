package org.vstu.compprehension.adapters.lti;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.vstu.compprehension.data.lti.LtiCourseMemberData;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.enums.LtiRegistrationMethod;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LtiMembershipClientTest {

    private static final String LMS = "https://lms.test";
    private static final String TOKEN_ENDPOINT = LMS + "/mod/lti/token.php";
    private static final String MEMBERSHIPS_URL = LMS + "/mod/lti/services.php/CourseSection/2/bindings/1/memberships";
    private static final String NEXT_PAGE_URL = MEMBERSHIPS_URL + "?limit=1&from=1";
    private static final String INSTRUCTOR = "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";
    private static final String LEARNER = "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";
    private static final LtiRegistrationData TOOL = new LtiRegistrationData(
            1, 1, LMS, LMS, "course-tool", null, null, LtiRegistrationMethod.LINK,
            LMS + "/mod/lti/auth.php", TOKEN_ENDPOINT, new LtiPlatformKeyData.Jwks(LMS + "/mod/lti/certs.php"),
            Instant.now());

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer lms = MockRestServiceServer.bindTo(restTemplate).build();
    private final LtiMembershipClient client = new LtiMembershipClient(
            restTemplate, new LtiServiceTokenClient(restTemplate, new LtiToolKeyProvider(toolProperties())));

    /** Участники собираются со всех страниц списка, а не только с первой. */
    @Test
    void membersAreCollectedFromAllPages() {
        // Arrange.
        expectToken();
        expectPage(MEMBERSHIPS_URL, NEXT_PAGE_URL, """
                {"members": [{"user_id": "5", "roles": ["%s"]}]}""".formatted(INSTRUCTOR));
        expectPage(NEXT_PAGE_URL, null, """
                {"members": [{"user_id": "6", "roles": ["%s"]}]}""".formatted(LEARNER));

        // Act.
        var members = client.fetchMembers(TOOL, MEMBERSHIPS_URL);

        // Assert.
        lms.verify();
        assertEquals(List.of(
                new LtiCourseMemberData("5", List.of(INSTRUCTOR), true),
                new LtiCourseMemberData("6", List.of(LEARNER), true)), members);
    }

    /** LMS отдаёт список только инструменту, получившему токен со scope NRPS, — от своего имени и своим ключом. */
    @Test
    void tokenIsRequestedWithMembershipScopeOnBehalfOfTool() {
        // Arrange.
        lms.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    var form = readForm((MockClientHttpRequest) request);
                    assertEquals(LtiMembershipClient.SCOPE, form.getFirst("scope"));
                    assertEquals(TOOL.clientId(), readSubject(form.getFirst("client_assertion")));
                })
                .andRespond(withSuccess("{\"access_token\": \"lms-token\"}", MediaType.APPLICATION_JSON));
        lms.expect(requestTo(MEMBERSHIPS_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer lms-token"))
                .andExpect(header(HttpHeaders.ACCEPT, "application/vnd.ims.lti-nrps.v2.membershipcontainer+json"))
                .andRespond(withSuccess("{\"members\": []}", MediaType.APPLICATION_JSON));

        // Act.
        client.fetchMembers(TOOL, MEMBERSHIPS_URL);

        // Assert.
        lms.verify();
    }

    /** Отчисленные и удалённые из курса — неактивные участники; без статуса участник активен. */
    @Test
    void inactiveAndDeletedMembersAreNotActive() {
        // Arrange.
        expectToken();
        expectPage(MEMBERSHIPS_URL, null, """
                {"members": [
                  {"user_id": "1", "roles": [], "status": "Active"},
                  {"user_id": "2", "roles": [], "status": "Inactive"},
                  {"user_id": "3", "roles": [], "status": "Deleted"},
                  {"user_id": "4", "roles": []}
                ]}""");

        // Act.
        var members = client.fetchMembers(TOOL, MEMBERSHIPS_URL);

        // Assert.
        assertEquals(List.of(true, false, false, true), members.stream().map(LtiCourseMemberData::active).toList());
    }

    /** Сбой LMS на любой странице — ошибка, а не неполный список: по неполному списку роли отнялись бы зря. */
    @Test
    void lmsFailureIsReportedInsteadOfPartialList() {
        // Arrange.
        expectToken();
        expectPage(MEMBERSHIPS_URL, NEXT_PAGE_URL, """
                {"members": [{"user_id": "5", "roles": ["%s"]}]}""".formatted(INSTRUCTOR));
        lms.expect(requestTo(NEXT_PAGE_URL)).andRespond(withServerError());

        // Act & Assert.
        assertThrows(RestClientException.class, () -> client.fetchMembers(TOOL, MEMBERSHIPS_URL));
    }

    /** Токен доступа к списку не уходит на чужой хост, даже если LMS сослалась туда как на следующую страницу. */
    @Test
    void nextPageOnAnotherHostIsRejected() {
        // Arrange.
        expectToken();
        expectPage(MEMBERSHIPS_URL, "https://other-host.test/memberships?from=1", """
                {"members": [{"user_id": "5", "roles": ["%s"]}]}""".formatted(INSTRUCTOR));

        // Act & Assert.
        assertThrows(IllegalStateException.class, () -> client.fetchMembers(TOOL, MEMBERSHIPS_URL));
        lms.verify();
    }

    /** Зациклившийся у LMS список страниц — ошибка, а не бесконечная загрузка. */
    @Test
    void endlessPagingIsStopped() {
        // Arrange.
        expectToken();
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.LINK, "<" + MEMBERSHIPS_URL + ">; rel=\"next\"");
        lms.expect(ExpectedCount.manyTimes(), requestTo(MEMBERSHIPS_URL))
                .andRespond(withSuccess("{\"members\": []}", MediaType.APPLICATION_JSON).headers(headers));

        // Act & Assert.
        assertThrows(IllegalStateException.class, () -> client.fetchMembers(TOOL, MEMBERSHIPS_URL));
    }

    private void expectToken() {
        lms.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withSuccess("{\"access_token\": \"lms-token\"}", MediaType.APPLICATION_JSON));
    }

    private void expectPage(String url, String nextPageUrl, String body) {
        var headers = new HttpHeaders();
        if (nextPageUrl != null) {
            headers.add(HttpHeaders.LINK, "<" + nextPageUrl + ">; rel=\"next\"");
        }
        lms.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON).headers(headers));
    }

    private static LtiToolProperties toolProperties() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var properties = new LtiToolProperties();
            properties.setToolPrivateKeyPkcs8Base64(
                    Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded()));
            return properties;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static MultiValueMap<String, String> readForm(MockClientHttpRequest request) {
        var form = UriComponentsBuilder.newInstance()
                .query(request.getBodyAsString(StandardCharsets.UTF_8)).build().getQueryParams();
        var decoded = new LinkedMultiValueMap<String, String>();
        form.forEach((name, values) -> values.forEach(
                value -> decoded.add(name, URLDecoder.decode(value, StandardCharsets.UTF_8))));
        return decoded;
    }

    private static String readSubject(String jwt) {
        try {
            return SignedJWT.parse(jwt).getJWTClaimsSet().getSubject();
        } catch (ParseException ex) {
            throw new AssertionError("client_assertion is not a JWT", ex);
        }
    }
}
