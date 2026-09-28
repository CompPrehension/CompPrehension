package org.vstu.compprehension.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OidcLogoutHandlerTest {

    private static final String REGISTRATION_ID = "sso";
    private static final String END_SESSION_ENDPOINT = "https://sso.test/realms/app/logout";
    private static final String ID_TOKEN = "user-id-token";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer provider = MockRestServiceServer.bindTo(restTemplate).build();

    /** Выход завершает сессию и у провайдера входа — по адресу, который провайдер опубликовал в discovery. */
    @Test
    void logoutEndsSessionAtPublishedProviderEndpoint() {
        // Arrange.
        var handler = new OidcLogoutHandler(restTemplate, new InMemoryClientRegistrationRepository(
                registration(Map.of("end_session_endpoint", END_SESSION_ENDPOINT))));
        provider.expect(requestTo(END_SESSION_ENDPOINT + "?id_token_hint=" + ID_TOKEN))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        // Act.
        handler.logout(new MockHttpServletRequest(), new MockHttpServletResponse(), signedInUser());

        // Assert.
        provider.verify();
    }

    /** Провайдер, не поддерживающий выход по инициативе клиента, не опрашивается — выход остаётся локальным. */
    @Test
    void logoutSkipsProviderWithoutEndSessionEndpoint() {
        // Arrange.
        var handler = new OidcLogoutHandler(restTemplate, new InMemoryClientRegistrationRepository(
                registration(Map.of())));

        // Act.
        handler.logout(new MockHttpServletRequest(), new MockHttpServletResponse(), signedInUser());

        // Assert.
        provider.verify();
    }

    /** Недоступный провайдер входа не мешает выйти локально. */
    @Test
    void logoutSurvivesProviderFailure() {
        // Arrange.
        var handler = new OidcLogoutHandler(restTemplate, new InMemoryClientRegistrationRepository(
                registration(Map.of("end_session_endpoint", END_SESSION_ENDPOINT))));
        provider.expect(requestTo(END_SESSION_ENDPOINT + "?id_token_hint=" + ID_TOKEN))
                .andRespond(withServerError());

        // Act & Assert.
        assertDoesNotThrow(() -> handler.logout(new MockHttpServletRequest(), new MockHttpServletResponse(), signedInUser()));
        provider.verify();
    }

    private static ClientRegistration registration(Map<String, Object> providerMetadata) {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("app")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://sso.test/auth")
                .tokenUri("https://sso.test/token")
                .providerConfigurationMetadata(providerMetadata)
                .build();
    }

    private static OAuth2AuthenticationToken signedInUser() {
        var idToken = new OidcIdToken(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "user"));
        return new OAuth2AuthenticationToken(new DefaultOidcUser(Set.of(), idToken), Set.of(), REGISTRATION_ID);
    }
}
