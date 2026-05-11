package org.vstu.compprehension.controllers;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.apache.commons.lang3.tuple.Pair;
import org.vstu.compprehension.Service.CourseService;
import org.vstu.compprehension.Service.EducationResourceService;
import org.vstu.compprehension.service.lti.LtiContextInitializer;
import org.vstu.compprehension.Service.LtiContextProvider;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.models.businesslogic.lti.LtiContext;
import org.vstu.compprehension.models.businesslogic.lti.LtiCourseContext;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;
import org.vstu.compprehension.utils.HttpRequestHelper;
import org.vstu.compprehension.utils.SessionHelper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("lti")
@Log4j2
public class LtiController {
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;
    private final LtiRegistrationsProperties ltiRegistrations;
    private final CourseService courseService;
    private final EducationResourceService educationResourceService;
    private final LtiContextInitializer ltiContextInitializer;
    private final LtiContextProvider ltiContextProvider;

    public LtiController(SecurityContextRepository securityContextRepository,
                         SecurityContextHolderStrategy securityContextHolderStrategy,
                         LtiRegistrationsProperties ltiRegistrations,
                         CourseService courseService,
                         EducationResourceService educationResourceService,
                         LtiContextInitializer ltiContextInitializer,
                         LtiContextProvider ltiContextProvider) {
        this.securityContextRepository = securityContextRepository;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
        this.ltiRegistrations = ltiRegistrations;
        this.courseService = courseService;
        this.educationResourceService = educationResourceService;
        this.ltiContextInitializer = ltiContextInitializer;
        this.ltiContextProvider = ltiContextProvider;
    }

    @SneakyThrows
    @GetMapping(value = "1_3/jwks", produces = "application/json")
    @ResponseBody
    public String jwks() {
        KeyFactory rsa = KeyFactory.getInstance("RSA");
        var jwks = ltiRegistrations.getRegistrations().entrySet().stream()
                .map(e -> buildJwk(e.getKey(), e.getValue().getPrivateKeyPkcs8Base64(), rsa))
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
        // TODO validate params

        String redirectUrl = String.format(
                "%s/mod/lti/auth.php?client_id=%s&response_type=%s&scope=%s&redirect_uri=%s&login_hint=%s&nonce=%s&state=%s&lti_message_hint=%s&response_mode=%s",
                params.issuer,
                URLEncoder.encode(params.clientId, StandardCharsets.UTF_8),
                "id_token",
                "openid",
                URLEncoder.encode(params.targetLinkUri, StandardCharsets.UTF_8),
                URLEncoder.encode(params.loginHint, StandardCharsets.UTF_8),
                UUID.randomUUID(),
                UUID.randomUUID(),
                URLEncoder.encode(params.ltiMessageHint, StandardCharsets.UTF_8),
                "form_post");

        log.info("LTI auth url created : {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    @SneakyThrows
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/exercise"})
    public void exercise(@RequestParam(required = false) Long id, HttpServletRequest request, HttpServletResponse response) {
        authenticateFromLti13ResourceLinkRequest(request, response);

        LtiContext ctx = ltiContextProvider.getCurrentLtiContext()
                .orElseThrow(() -> new IllegalArgumentException("LTI context absent"));

        Pair<Long, Long> exerciseAndCourse = resolveExerciseAndCourse(ctx, id);

        Long exId = exerciseAndCourse.getLeft();
        Long courseId = exerciseAndCourse.getRight();

        String redirectUrl = String.format(
                "/pages/exercise?exerciseId=%d&courseId=%d",
                exId, courseId
        );
        log.info("Redirect to exercise, url:{}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * @return (exerciseId, courseId)
     */
    private Pair<Long, Long> resolveExerciseAndCourse(LtiContext ctx, Long fallbackExerciseId) {
        Long exerciseId = ctx.exerciseId() != null ? ctx.exerciseId() : fallbackExerciseId;
        if (exerciseId == null)
            throw new IllegalArgumentException("exerciseId is not provided: set custom parameter 'exercise_id' in Moodle activity or use ?id= query param");

        Long courseId = resolveCourseFromContext(ctx);
        if (courseId == null)
            throw new IllegalArgumentException("Absent information on the contextId");

        courseService.linkExerciseWithCourseIfMissing(exerciseId, courseId);
        return Pair.of(exerciseId, courseId);
    }

    @SneakyThrows
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/exercise-settings"})
    public void exerciseSettings(HttpServletRequest request, HttpServletResponse response) {
        authenticateFromLti13ResourceLinkRequest(request, response);

        Long courseId = ltiContextProvider.getCurrentLtiContext()
                .map(this::resolveCourseFromContext)
                .orElse(null);

        String redirectUrl = String.format("/pages/exercise-settings?courseId=%s", courseId);
        log.info("Redirect to exercise-settings, url:{}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private Long resolveCourseFromContext(LtiContext ctx) {
        LtiCourseContext ltiCourse = ctx.course();
        if (ltiCourse == null || ltiCourse.courseId() == null) return null;

        EducationResourceEntity eduResource = educationResourceService.findByUrlAndType(ctx.lmsUrl(), ctx.lmsType())
                .orElseGet(() -> educationResourceService.createOrGetExisting(ctx.lmsUrl(), ctx.lmsType()));

        String externalCourseId = ltiCourse.courseId();
        String courseName = ltiCourse.courseName() != null ? ltiCourse.courseName() : "id_" + externalCourseId;
        CourseEntity course = courseService.findByExternalIdAndResourceId(externalCourseId, eduResource.getId())
                .orElseGet(() -> courseService.createOrGetExisting(externalCourseId, courseName, eduResource.getId()));

        return course.getId();
    }

    private void authenticateFromLti13ResourceLinkRequest(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, ParseException {
        Map<String, String> formDataParams = HttpRequestHelper.getAllRequestParams(request);
        String rawIdToken = formDataParams.get("id_token");
        if (StringHelper.isNullOrWhitespace(rawIdToken)) {
            throw new AuthenticationServiceException("No 'id_token' inside request params");
        }

        // TECH DEBT: JWT signature is NOT validated (no JWKS fetch from Moodle).
        // Full validation requires fetching Moodle's JWKS via issuer's .well-known/openid-configuration.
        JWT idToken = JWTParser.parse(rawIdToken);
        final Map<String, Object> claims = idToken.getJWTClaimsSet().getClaims();

        OidcIdToken oidcToken = new OidcIdToken(rawIdToken,
                idToken.getJWTClaimsSet().getIssueTime().toInstant(),
                idToken.getJWTClaimsSet().getExpirationTime().toInstant(),
                idToken.getJWTClaimsSet().getClaims());
        JSONArray groups = (JSONArray) claims.get(LTI_CLAIM_ROLES);
        Set<SimpleGrantedAuthority> mappedAuthorities = groups.stream()
                .map(role -> new SimpleGrantedAuthority(Arrays.stream(role.toString().split("#"))
                        .reduce((first, second) -> second)
                        .map(r -> "ROLE_" + r)
                        .orElseThrow()))
                .collect(Collectors.toSet());
        OAuth2User user = new DefaultOidcUser(mappedAuthorities, oidcToken);
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(user, mappedAuthorities, "mdl");

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        ltiContextInitializer.init(claims);

        log.info("user '{}:{}' is successfully authenticated from LTI with authorities {}", oidcToken.getFullName(), user.getName(), mappedAuthorities);
    }
}
