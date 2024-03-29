package org.vstu.compprehension.controllers;

import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.utils.HttpRequestHelper;
import org.vstu.compprehension.utils.SessionHelper;

import javax.security.sasl.AuthenticationException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("lti")
@Log4j2
public class LtiController {
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    public LtiController(SecurityContextRepository securityContextRepository, SecurityContextHolderStrategy securityContextHolderStrategy) {
        this.securityContextRepository     = securityContextRepository;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

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
    public void exercise(@RequestParam long id, HttpServletRequest request, HttpServletResponse response) {
        authenticateFromLti13ResourceLinkRequest(request, response);

        var redirectUrl = String.format("/pages/exercise?exerciseId=%d", id);
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
        var rawState = formDataParams.get("state");
        if (StringHelper.isNullOrWhitespace(rawIdToken)) {
            throw new AuthenticationException("No 'id_token' inside request params");
        }

        var idToken = JWTParser.parse(rawIdToken);
        final Map<String, Object> claims = idToken.getJWTClaimsSet().getClaims();
        var oidcToken = new OidcIdToken(rawIdToken,
                idToken.getJWTClaimsSet().getIssueTime().toInstant(),
                idToken.getJWTClaimsSet().getExpirationTime().toInstant(),
                idToken.getJWTClaimsSet().getClaims());
        var groups = (JSONArray) claims.get("https://purl.imsglobal.org/spec/lti/claim/roles");
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

        log.info("user '{}:{}' is successfully authenticated from LTI with authorities {}", oidcToken.getFullName(), user.getName(), mappedAuthorities);
    }
}
