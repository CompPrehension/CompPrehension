package org.vstu.compprehension.controllers;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.CourseFrontendService;
import org.vstu.compprehension.frontend.EducationResourceFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.course.CreateCourseDto;
import org.vstu.compprehension.service.lti.DeepLinkingResponseService;
import org.vstu.compprehension.service.lti.LtiContextInitializer;
import org.vstu.compprehension.service.lti.LtiIdTokenVerifier;
import org.vstu.compprehension.service.lti.LtiPendingLogins;
import org.vstu.compprehension.service.lti.LtiRegistrationRegistry;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemPermission;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.businesslogic.lti.LtiContext;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.services.LtiContextProvider;
import org.vstu.compprehension.utils.HttpRequestHelper;
import org.vstu.compprehension.utils.SessionHelper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("lti")
@Log4j2
@RequiredArgsConstructor
public class LtiController {
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;
    private final LtiRegistrationRegistry ltiRegistrations;
    private final CourseFrontendService courseService;
    private final EducationResourceFrontendService educationResourceFacade;
    private final LtiContextInitializer ltiContextInitializer;
    private final LtiIdTokenVerifier ltiIdTokenVerifier;
    private final LtiPendingLogins ltiPendingLogins;
    private final LtiContextProvider ltiProvider;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;

    @SneakyThrows
    @GetMapping(value = "1_3/jwks", produces = "application/json")
    @ResponseBody
    public String jwks() {
        KeyFactory rsa = KeyFactory.getInstance("RSA");
        var jwks = ltiRegistrations.getToolPrivateKeysByKeyId().entrySet().stream()
                .map(e -> buildJwk(e.getKey(), e.getValue(), rsa))
                .toList();
        return new JWKSet(jwks).toString();
    }

    @SneakyThrows
    private JWK buildJwk(String kid, String privateKeyBase64, KeyFactory rsa) {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) rsa.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
        RSAPublicKey publicKey = (RSAPublicKey) rsa.generatePublic(publicKeySpec);
        return new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();
    }

    // LTI 1.3 standard claim URLs
    private static final String LTI_CLAIM_ROLES = "https://purl.imsglobal.org/spec/lti/claim/roles";
    private static final String LTI_CLAIM_MESSAGE_TYPE = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    private static final String LTI_CLAIM_CUSTOM = "https://purl.imsglobal.org/spec/lti/claim/custom";
    private static final String LTI_MESSAGE_TYPE_DEEP_LINKING = "LtiDeepLinkingRequest";

    @Data
    @Builder
    public static class LtiOidcLoginRequest {
        private String ltiDeploymentId;
        private String clientId;
        private String issuer;
        private String loginHint;
        private String ltiMessageHint;
        private String targetLinkUri;
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.POST, path = {"1_3/login"})
    public void login1_3(HttpServletRequest request, HttpServletResponse response) {
        SessionHelper.ensureNewSession(request);

        Map<String, String> formDataParams = HttpRequestHelper.getAllRequestParams(request);
        LtiOidcLoginRequest params = LtiOidcLoginRequest.builder()
                .ltiDeploymentId(formDataParams.get("lti_deployment_id"))
                .clientId(formDataParams.get("client_id"))
                .issuer(formDataParams.get("iss"))
                .loginHint(formDataParams.get("login_hint"))
                .ltiMessageHint(formDataParams.get("lti_message_hint"))
                .targetLinkUri(formDataParams.get("target_link_uri"))
                .build();

        if (params.issuer == null) {
            throw new SecurityException("LTI login has no issuer");
        }
        var platform = ltiRegistrations.findByIssuer(params.issuer)
                .orElseThrow(() -> new SecurityException(String.format("LTI issuer %s is not registered", params.issuer)));
        if (!platform.clientId().equals(params.clientId)) {
            throw new SecurityException(String.format("LTI client_id %s is not registered for issuer %s", params.clientId, params.issuer));
        }
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        ltiPendingLogins.savePendingLogin(state, nonce);

        String authorizationEndpoint = platform.authorizationEndpoint();
        String redirectUrl = String.format(
                "%s%sclient_id=%s&response_type=%s&scope=%s&redirect_uri=%s&login_hint=%s&nonce=%s&state=%s&lti_message_hint=%s&response_mode=%s",
                authorizationEndpoint,
                authorizationEndpoint.contains("?") ? "&" : "?",
                URLEncoder.encode(params.clientId, StandardCharsets.UTF_8),
                "id_token",
                "openid",
                URLEncoder.encode(params.targetLinkUri, StandardCharsets.UTF_8),
                URLEncoder.encode(params.loginHint, StandardCharsets.UTF_8),
                nonce,
                state,
                URLEncoder.encode(params.ltiMessageHint, StandardCharsets.UTF_8),
                "form_post");

        log.info("LTI auth url created : {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /** Единая точка запуска: куда вести, решает сам запуск (см. {@link #resolveLaunchTarget}). */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/launch"})
    public void launch(@RequestParam(required = false) Long id, HttpServletRequest request, HttpServletResponse response) {
        handleLaunch(LaunchTarget.EXERCISE, id, request, response);
    }

    // Старые адреса запуска: на них указывают уже созданные в LMS активности.

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/exercise"})
    public void exercise(@RequestParam(required = false) Long id, HttpServletRequest request, HttpServletResponse response) {
        handleLaunch(LaunchTarget.EXERCISE, id, request, response);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/exercise-settings"})
    public void exerciseSettings(HttpServletRequest request, HttpServletResponse response) {
        handleLaunch(LaunchTarget.EXERCISE_SETTINGS, null, request, response);
    }

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/configure-course"})
    public void configureCourse(HttpServletRequest request, HttpServletResponse response) {
        handleLaunch(LaunchTarget.DEEP_LINKING, null, request, response);
    }

    private enum LaunchTarget {
        EXERCISE,
        EXERCISE_SETTINGS,
        DEEP_LINKING
    }

    @SneakyThrows
    private void handleLaunch(@NotNull LaunchTarget defaultTarget, @Nullable Long fallbackExerciseId,
                              HttpServletRequest request, HttpServletResponse response) {
        Jwt idToken = authenticateFromLti13ResourceLinkRequest(request, response);
        LtiContext ctx = ltiProvider.getCurrentLtiContext()
                .orElseThrow(() -> new IllegalArgumentException("LTI context absent"));

        String redirectUrl = switch (resolveLaunchTarget(idToken, defaultTarget)) {
            case EXERCISE -> resolveExerciseUrl(ctx, fallbackExerciseId);
            case EXERCISE_SETTINGS -> resolveExerciseSettingsUrl(ctx);
            case DEEP_LINKING -> resolveDeepLinkingUrl(ctx);
        };
        log.info("LTI launch redirect, url:{}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private static @NotNull LaunchTarget resolveLaunchTarget(@NotNull Jwt idToken, @NotNull LaunchTarget defaultTarget) {
        if (LTI_MESSAGE_TYPE_DEEP_LINKING.equals(idToken.getClaimAsString(LTI_CLAIM_MESSAGE_TYPE))) {
            return LaunchTarget.DEEP_LINKING;
        }
        Map<String, Object> custom = idToken.getClaimAsMap(LTI_CLAIM_CUSTOM);
        if (custom != null && DeepLinkingResponseService.CUSTOM_PAGE_EXERCISE_SETTINGS
                .equals(custom.get(DeepLinkingResponseService.CUSTOM_PAGE))) {
            return LaunchTarget.EXERCISE_SETTINGS;
        }
        return defaultTarget;
    }

    private @NotNull String resolveExerciseUrl(@NotNull LtiContext ctx, @Nullable Long fallbackExerciseId) {
        Long exerciseId = ctx.exerciseId() != null ? ctx.exerciseId() : fallbackExerciseId;
        if (exerciseId == null)
            throw new IllegalArgumentException("exerciseId is not provided: set custom parameter 'exercise_id' in Moodle activity or use ?id= query param");

        Long courseId = resolveCourseFromContext(ctx);
        if (courseId == null)
            throw new IllegalArgumentException("Absent information on the contextId");

        courseService.linkExerciseWithCourseIfMissing(exerciseId, courseId);
        return String.format("/pages/exercise?exerciseId=%d&courseId=%d", exerciseId, courseId);
    }

    private @NotNull String resolveExerciseSettingsUrl(@NotNull LtiContext ctx) {
        return String.format("/pages/exercise-settings?courseId=%s", resolveCourseFromContext(ctx));
    }

    private @NotNull String resolveDeepLinkingUrl(@NotNull LtiContext ctx) {
        // Триггерит upsert пользователя + назначение RBAC-роли (LTI Instructor -> Teacher в scope курса).
        long userId = userService.getCurrentUserId();

        Long courseId = resolveCourseFromContext(ctx);
        if (courseId == null) {
            throw new IllegalArgumentException("Absent information on the contextId");
        }
        authService.ensureAuthorized(userId, SystemPermission.CREATE_LMS_ACTIVITY, authService.getCourseScope(courseId));
        return String.format("/pages/course?courseId=%d&lti=deeplink", courseId);
    }

    private Long resolveCourseFromContext(LtiContext ctx) {
        if (ctx.course() == null || ctx.course().courseId() == null) return null;

        long eduResourceId = getOrCreateTrustedEducationResourceId(ctx);
        return courseService.getOrCreate(new CreateCourseDto(eduResourceId, ctx.course().courseId(), ctx.course().courseName()));
    }

    private long getOrCreateTrustedEducationResourceId(LtiContext ctx) {
        if (ctx.lmsUrl() == null) {
            throw new SecurityException("LTI launch has no valid issuer url");
        }
        // Подпись запуска уже проверена ключом зарегистрированной LMS, поэтому новая LMS сразу доверенная.
        // Статус уже известной LMS не меняется: UNTRUSTED и BANNED остаются закрытыми.
        var educationResource = educationResourceFacade.getOrCreate(
                ctx.lmsUrl(), ctx.lmsType(), EducationResourceTrustStatus.TRUSTED);
        if (educationResource.trustStatus() != EducationResourceTrustStatus.TRUSTED) {
            throw new SecurityException(String.format("EducationResource %s is not trusted", educationResource.url()));
        }
        return educationResource.id();
    }

    private @NotNull Jwt authenticateFromLti13ResourceLinkRequest(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        Map<String, String> formDataParams = HttpRequestHelper.getAllRequestParams(request);
        String rawIdToken = formDataParams.get("id_token");
        if (StringHelper.isNullOrWhitespace(rawIdToken)) {
            throw new AuthenticationServiceException("No 'id_token' inside request params");
        }

        Jwt idToken = ltiIdTokenVerifier.verify(rawIdToken);
        ensureLaunchStartedByOwnLogin(formDataParams.get("state"), idToken);
        final Map<String, Object> claims = idToken.getClaims();

        OidcIdToken oidcToken = new OidcIdToken(rawIdToken, idToken.getIssuedAt(), idToken.getExpiresAt(), claims);
        if (!(claims.get(LTI_CLAIM_ROLES) instanceof Collection<?> groups)) {
            throw new AuthenticationServiceException("Claim '" + LTI_CLAIM_ROLES + "' is required inside id_token");
        }
        Set<SimpleGrantedAuthority> mappedAuthorities = groups.stream()
                .map(role -> new SimpleGrantedAuthority(Arrays.stream(role.toString().split("#"))
                        .reduce((first, second) -> second)
                        .map(r -> "ROLE_" + r)
                        .orElseThrow()))
                .collect(Collectors.toSet());
        OAuth2User user = new DefaultOidcUser(mappedAuthorities, oidcToken);
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(user, mappedAuthorities, "mdl");

        ltiContextInitializer.init(claims);
        try {
            ltiProvider.getCurrentLtiContext().ifPresent(this::getOrCreateTrustedEducationResourceId);
        } catch (SecurityException ex) {
            // Контекст сессионный: без очистки в уже аутентифицированной сессии остался бы запуск из недоверенной LMS.
            ltiContextInitializer.clear();
            throw ex;
        }

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        log.info("user '{}:{}' is successfully authenticated from LTI with authorities {}", oidcToken.getFullName(), user.getName(), mappedAuthorities);
        return idToken;
    }

    private void ensureLaunchStartedByOwnLogin(@Nullable String state, @NotNull Jwt idToken) {
        String expectedNonce = state == null ? null : ltiPendingLogins.takeNonce(state);
        if (expectedNonce == null || !expectedNonce.equals(idToken.getClaimAsString("nonce"))) {
            throw new SecurityException("LTI launch does not match a login started by this tool");
        }
    }
}
