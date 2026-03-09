package org.vstu.compprehension.controllers;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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
import org.vstu.compprehension.common.StringHelper;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("lti")
@Log4j2
public class LtiController {
    /** Key ID used in JWKS and JWT assertions — must match between both. */
    public static final String KID = "compph-lti-1";

    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    @Value("${lti.private-key-pkcs8-base64:}")
    private String privateKeyBase64;

    public LtiController(SecurityContextRepository securityContextRepository, SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.securityContextRepository     = securityContextRepository;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    /**
     * Returns the tool's RSA public key as a JWK Set.
     * Configure Moodle LTI tool with "Public key type: JWK keyset URL" pointing here.
     * The public key is derived on-the-fly from the configured private key.
     */
    @SneakyThrows
    @GetMapping(value = "1_3/jwks", produces = "application/json")
    @ResponseBody
    public String jwks() {
        var keyBytes = Base64.getDecoder().decode(privateKeyBase64);
        var privateKey = (RSAPrivateCrtKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        var publicKeySpec = new RSAPublicKeySpec(
                privateKey.getModulus(), privateKey.getPublicExponent());
        var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);

        var rsaKey = new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(KID)
                .build();
        return new JWKSet(rsaKey).toString();
    }

    // LTI 1.3 standard claim URLs
    private static final String LTI_CLAIM_ROLES   = "https://purl.imsglobal.org/spec/lti/claim/roles";
    private static final String LTI_CLAIM_CONTEXT = "https://purl.imsglobal.org/spec/lti/claim/context";
    private static final String LTI_CLAIM_AGS     = "https://purl.imsglobal.org/spec/lti-ags/claim/endpoint";
    private static final String LTI_CLAIM_CUSTOM  = "https://purl.imsglobal.org/spec/lti/claim/custom";

    @Data @Builder
    public static class LtiOidcLoginRequest {
        private String ltiDeploymentId;
        private String clientId;
        private String issuer;
        private String loginHint;
        private String ltiMessageHint;
        private String targetLinkUri;
    }

    @SneakyThrows
    @RequestMapping(method = RequestMethod.POST, path = {"1_3/login"} )
    public void login1_3(HttpServletRequest request, HttpServletResponse response) {
        SessionHelper.ensureNewSession(request);

        var formDataParams = HttpRequestHelper.getAllRequestParams(request);
        var params = LtiOidcLoginRequest.builder()
                .ltiDeploymentId(formDataParams.get("lti_deployment_id"))
                .clientId(formDataParams.get("client_id"))
                .issuer(formDataParams.get("iss"))
                .loginHint(formDataParams.get("login_hint"))
                .ltiMessageHint(formDataParams.get("lti_message_hint"))
                .targetLinkUri(formDataParams.get("target_link_uri"))
                .build();
        // TODO validate params

        var redirectUrl = String.format(
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
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/exercise"} )
    public void exercise(@RequestParam(required = false) Long id, HttpServletRequest request, HttpServletResponse response) {
        authenticateFromLti13ResourceLinkRequest(request, response);

        // Prefer exercise_id from LTI custom parameters (set per-activity in Moodle).
        // Fall back to query param ?id= for backward compatibility.
        Long exerciseId = (Long) request.getSession().getAttribute("ltiExerciseId");
        if (exerciseId == null) exerciseId = id;
        if (exerciseId == null) throw new IllegalArgumentException("exerciseId is not provided: set custom parameter 'exercise_id' in Moodle activity or use ?id= query param");

        var redirectUrl = String.format("/pages/exercise?exerciseId=%d", exerciseId);
        log.info("Redirect to exercise, url:{}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    @SneakyThrows
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, path = {"1_3/exercise-settings"} )
    public void exerciseSettings(HttpServletRequest request, HttpServletResponse response) {
        authenticateFromLti13ResourceLinkRequest(request, response);

        var redirectUrl = "/pages/exercise-settings";
        log.info("Redirect to exercise-settings, url:{}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void authenticateFromLti13ResourceLinkRequest(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, ParseException {
        var formDataParams = HttpRequestHelper.getAllRequestParams(request);
        var rawIdToken = formDataParams.get("id_token");
        if (StringHelper.isNullOrWhitespace(rawIdToken)) {
            throw new AuthenticationServiceException("No 'id_token' inside request params");
        }

        // TECH DEBT: JWT signature is NOT validated (no JWKS fetch from Moodle).
        // This is sufficient for diploma/demo purposes but must be fixed before production use.
        // Full validation requires fetching Moodle's JWKS via issuer's .well-known/openid-configuration.
        var idToken = JWTParser.parse(rawIdToken);
        final Map<String, Object> claims = idToken.getJWTClaimsSet().getClaims();
        var oidcToken = new OidcIdToken(rawIdToken,
                idToken.getJWTClaimsSet().getIssueTime().toInstant(),
                idToken.getJWTClaimsSet().getExpirationTime().toInstant(),
                idToken.getJWTClaimsSet().getClaims());
        var groups = (JSONArray) claims.get(LTI_CLAIM_ROLES);
        var mappedAuthorities = groups.stream()
                .map(role -> new SimpleGrantedAuthority(Arrays.stream(role.toString().split("#"))
                        .reduce((first, second) -> second)
                        .map(r -> "ROLE_" + r)
                        .orElseThrow()))
                .collect(Collectors.toSet());
        OAuth2User user = new DefaultOidcUser(mappedAuthorities, oidcToken);
        var authentication = new OAuth2AuthenticationToken(user, mappedAuthorities, "mdl");

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // Save LTI AGS context to session — used when creating an exercise attempt
        var agsEndpoint = (Map<?, ?>) claims.get(LTI_CLAIM_AGS);
        if (agsEndpoint != null && agsEndpoint.get("lineitem") != null) {
            var lineitemUrl = String.valueOf(agsEndpoint.get("lineitem"));
            request.getSession().setAttribute("ltiLineitemUrl", lineitemUrl);
            log.info("LTI lineitem URL saved to session: {}", lineitemUrl);
        }
        var ltiContext = (Map<?, ?>) claims.get(LTI_CLAIM_CONTEXT);
        if (ltiContext != null) {
            request.getSession().setAttribute("ltiContextId", String.valueOf(ltiContext.get("id")));
        }
        // Moodle internal user ID (JWT sub) — needed for grade passback, independent of account merging
        request.getSession().setAttribute("ltiUserId", idToken.getJWTClaimsSet().getSubject());

        // Extract exerciseId from LTI custom parameters (key: exercise_id).
        // Moodle activity sets this via the "Custom parameters" field: exercise_id=<id>
        var customClaims = (Map<?, ?>) claims.get(LTI_CLAIM_CUSTOM);
        if (customClaims != null) {
            var exerciseIdStr = (String) customClaims.get("exercise_id");
            if (exerciseIdStr != null) {
                request.getSession().setAttribute("ltiExerciseId", Long.parseLong(exerciseIdStr));
                log.info("LTI custom exercise_id={} saved to session", exerciseIdStr);
            }
        }

        log.info("user '{}:{}' is successfully authenticated from LTI with authorities {}", oidcToken.getFullName(), user.getName(), mappedAuthorities);
    }
}
