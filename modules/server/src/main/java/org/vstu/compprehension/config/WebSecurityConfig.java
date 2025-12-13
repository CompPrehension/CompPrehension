package org.vstu.compprehension.config;

import com.nimbusds.jose.shaded.json.JSONArray;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private final Optional<KeycloakLogoutHandler> keycloakLogoutHandler;
    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;
    private final Optional<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService;

    public WebSecurityConfig(Optional<KeycloakLogoutHandler> keycloakLogoutHandler,
                            Optional<ClientRegistrationRepository> clientRegistrationRepository,
                            Optional<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService) {
        this.keycloakLogoutHandler = keycloakLogoutHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.oidcUserService = oidcUserService;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
        http.csrf(AbstractHttpConfigurer::disable);
        
        // Настройка авторизации: если OAuth2 отключен, разрешаем все запросы
        if (clientRegistrationRepository.isPresent() && oidcUserService.isPresent()) {
            http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                    .requestMatchers(new AntPathRequestMatcher("/lti/**")).permitAll()
                    .anyRequest().authenticated())
                .oauth2Login(oauth2Login ->
                    oauth2Login.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                                .oidcUserService(oidcUserService.get())
                        )
                )
                .logout((logout) -> {
                    logout.logoutSuccessUrl("/pages/exercise-settings")
                          .invalidateHttpSession(true)
                          .clearAuthentication(true)
                          .deleteCookies("JSESSIONID");
                    keycloakLogoutHandler.ifPresent(logout::addLogoutHandler);
                });
        } else {
            // Когда OAuth2 отключен, разрешаем все запросы
            http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                    .anyRequest().permitAll())
                .logout((logout) -> logout.logoutSuccessUrl("/pages/exercise-settings")
                          .invalidateHttpSession(true)
                          .clearAuthentication(true)
                          .deleteCookies("JSESSIONID"));
        }
        
        http.exceptionHandling(c ->
                c.defaultAuthenticationEntryPointFor(getRestAuthenticationEntryPoint(), new AntPathRequestMatcher("/api/**")));
        return http.build();
    }

    private AuthenticationEntryPoint getRestAuthenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowOrigins = List.of("*");
        configuration.setAllowedOriginPatterns(allowOrigins);
        configuration.setAllowedMethods(singletonList("*"));
        configuration.setAllowedHeaders(singletonList("*"));
        //in case authentication is enabled this flag MUST be set, otherwise CORS requests will fail
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityContextRepository getSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityContextHolderStrategy getSecurityContextHolderStrategy() {
        return SecurityContextHolder.getContextHolderStrategy();
    }

    /*
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}/pages/exercise-settings");
        return successHandler;
    }
    */

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
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
