package org.vstu.compprehension.service.gradepassback;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.vstu.compprehension.data.exerciseattempt.GradePassbackTargetData;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;
import org.vstu.compprehension.enums.LtiRegistrationMethod;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;
import org.vstu.compprehension.repositories.entity.LtiRegistrationRepository;
import org.vstu.compprehension.service.lti.LtiRegistrationRegistry;
import org.vstu.compprehension.service.lti.LtiTokenService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Transactional
class LtiAgsGradePassbackStrategyTest extends AbstractIntegrationTest {

    private static final String LMS = TestData.EducationResources.URL;
    private static final String TOKEN_ENDPOINT = LMS + "/mod/lti/token.php";
    private static final String LINEITEM = LMS + "/mod/lti/services.php/2/lineitems/3/lineitem?type_id=1";

    @Autowired private LtiRegistrationRegistry ltiRegistrations;
    @Autowired private LtiRegistrationRepository ltiRegistrationRepository;

    private MockRestServiceServer lms;
    private LtiAgsGradePassbackStrategy strategy;

    @BeforeEach
    void setUpFakeLms() {
        // Свой RestTemplate: общий бин из контекста подменять нельзя, контекст переиспользуется другими тестами.
        var restTemplate = new RestTemplate();
        lms = MockRestServiceServer.bindTo(restTemplate).build();
        strategy = new LtiAgsGradePassbackStrategy(restTemplate, new LtiTokenService(restTemplate), ltiRegistrations);
    }

    /** У LMS два инструмента: токен для оценки просится от имени того, через который начата попытка. */
    @Test
    void gradeIsSentOnBehalfOfToolOfAttempt() {
        // Arrange.
        registerSecondTool(LMS, "course-tool");
        expectTokenRequestFrom("course-tool");
        expectScore();

        // Act.
        strategy.passGrade(target(LMS, "course-tool"), 0.8, Instant.now());

        // Assert.
        lms.verify();
    }

    /** Инструмент попытки ищется по issuer, с которым он подключён, даже если адрес колонки журнала записан иначе. */
    @Test
    void gradeIsSentThroughToolWithIssuerDifferentFromLineitemAddress() {
        // Arrange.
        registerSecondTool(LMS + "/", "slash-tool");
        expectTokenRequestFrom("slash-tool");
        expectScore();

        // Act.
        strategy.passGrade(target(LMS + "/", "slash-tool"), 0.8, Instant.now());

        // Assert.
        lms.verify();
    }

    /** Попытка, в которой не записан инструмент, отправляет оценку через единственный инструмент LMS. */
    @Test
    void gradeOfAttemptWithoutToolUsesTheOnlyTool() {
        // Arrange.
        expectTokenRequestFrom("test-client");
        expectScore();

        // Act.
        strategy.passGrade(target(null, null), 0.8, Instant.now());

        // Assert.
        lms.verify();
    }

    private void expectTokenRequestFrom(String clientId) {
        lms.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertEquals(clientId, readClientAssertionSubject((MockClientHttpRequest) request)))
                .andRespond(withSuccess("{\"access_token\": \"lms-token\"}", MediaType.APPLICATION_JSON));
    }

    private void expectScore() {
        lms.expect(requestTo(LMS + "/mod/lti/services.php/2/lineitems/3/lineitem/scores?type_id=1"))
                .andExpect(header("Authorization", "Bearer lms-token"))
                .andRespond(withSuccess());
    }

    private static String readClientAssertionSubject(MockClientHttpRequest request) throws IOException {
        var form = UriComponentsBuilder.newInstance()
                .query(request.getBodyAsString(StandardCharsets.UTF_8)).build().getQueryParams();
        var assertion = URLDecoder.decode(form.getFirst("client_assertion"), StandardCharsets.UTF_8);
        try {
            return SignedJWT.parse(assertion).getJWTClaimsSet().getSubject();
        } catch (ParseException ex) {
            throw new AssertionError("client_assertion is not a JWT", ex);
        }
    }

    private static GradePassbackTargetData target(String ltiIssuer, String ltiClientId) {
        return new GradePassbackTargetData(1, TestData.Exercises.MAIN_COURSE_ID, TestData.Users.MAIN_COURSE_STUDENT_ID,
                "moodle-user", LINEITEM, ltiIssuer, ltiClientId, null);
    }

    /** Ключ LMS тот же, что у регистрации из data.sql: у тестовой LMS нет JWKS. */
    private void registerSecondTool(String issuer, String clientId) {
        var registered = ltiRegistrationRepository.findByIssuerAndClientId(LMS, "test-client").orElseThrow();
        var tool = new LtiRegistrationEntity();
        tool.setEducationResource(registered.getEducationResource());
        tool.setIssuer(issuer);
        tool.setClientId(clientId);
        tool.setMethod(LtiRegistrationMethod.MANUAL);
        tool.setAuthorizationEndpoint(registered.getAuthorizationEndpoint());
        tool.setTokenEndpoint(TOKEN_ENDPOINT);
        tool.setPlatformPublicKey(registered.getPlatformPublicKey());
        ltiRegistrationRepository.saveAndFlush(tool);
    }
}
