package org.vstu.compprehension.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("lti")
public class LtiController {
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
    public void login1_3(HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String, String> requestParams) {
        var decodeCodec = new URLCodec();
        var rawBody = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
        var rawParams = Arrays.stream(rawBody.split("&"))
                .map(x -> x.split("="))
                .collect(Collectors.toMap(x -> x[0], x -> { try { return decodeCodec.decode(x[1]); } catch (Exception e) { return ""; }}));
        rawParams.putAll(requestParams);

        var params = LtiOidcLoginRequest.builder()
                .ltiDeploymentId(rawParams.get("lti_deployment_id"))
                .clientId(rawParams.get("client_id"))
                .issuer(rawParams.get("iss"))
                .loginHint(rawParams.get("login_hint"))
                .ltiMessageHint(rawParams.get("lti_message_hint"))
                .targetLinkUri(rawParams.get("target_link_uri"))
                .build();
        // TODO validate params

        var redirectUrl = String.format(
                "https://localhost/mod/lti/auth.php?client_id=%s&response_type=%s&scope=%s&redirect_uri=%s&login_hint=%s&nonce=%s&lti_message_hint=%s&response_mode=%s",
                URLEncoder.encode(params.clientId, StandardCharsets.UTF_8),
                "id_token",
                "openid",
                URLEncoder.encode(params.targetLinkUri, StandardCharsets.UTF_8),
                URLEncoder.encode(params.loginHint, StandardCharsets.UTF_8),
                UUID.randomUUID(),
                URLEncoder.encode(params.ltiMessageHint, StandardCharsets.UTF_8),
                "form_post");
        response.setHeader("Location", redirectUrl);
        response.setStatus(302);
    }
}
