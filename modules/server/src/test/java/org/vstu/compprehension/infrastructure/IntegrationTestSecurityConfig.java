package org.vstu.compprehension.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Заглушка регистрации OIDC на весь тестовый профиль.
 */
@Configuration
@Profile("test")
public class IntegrationTestSecurityConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId("keycloak")
                        .clientId("integration-test")
                        .clientSecret("integration-test")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri("http://localhost/oauth2/authorize")
                        .tokenUri("http://localhost/oauth2/token")
                        .userInfoUri("http://localhost/userinfo")
                        .userNameAttributeName("sub")
                        .jwkSetUri("http://localhost/jwks")
                        .scope("openid")
                        .build());
    }
}
