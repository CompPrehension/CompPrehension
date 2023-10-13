package org.vstu.compprehension.config;

import com.nimbusds.jose.shaded.json.JSONArray;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private final KeycloakLogoutHandler keycloakLogoutHandler;

    public WebSecurityConfig(KeycloakLogoutHandler keycloakLogoutHandler) {
        this.keycloakLogoutHandler = keycloakLogoutHandler;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(new AntPathRequestMatcher("/api/**"), new AntPathRequestMatcher("/pages/**")).authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2Login ->
                    oauth2Login.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                                .oidcUserService(this.oidcUserService())
                        )
                )
                .logout((logout) -> logout.addLogoutHandler(keycloakLogoutHandler)
                          .logoutSuccessUrl("/pages/exercise-settings")
                          .invalidateHttpSession(true)
                          .clearAuthentication(true)
                          .deleteCookies("JSESSIONID"));
        return http.build();
    }

    /*
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}/pages/exercise-settings");
        return successHandler;
    }
    */

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            final Map<String, Object> claims = oidcUser.getClaims();
            final JSONArray groups = (JSONArray)claims.get("groups");
            if (groups == null)
                throw new OAuth2AuthenticationException("Claim 'groups' is required for access_token");

            final Set<GrantedAuthority> mappedAuthorities = groups.stream()
                    .map(role -> new SimpleGrantedAuthority(("ROLE_" + role)))
                    .collect(Collectors.toSet());

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    /*
    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }
    */
}
