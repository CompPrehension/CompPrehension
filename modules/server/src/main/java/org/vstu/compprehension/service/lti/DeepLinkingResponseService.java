package org.vstu.compprehension.service.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.config.LtiRegistrationsProperties.Registration;
import org.vstu.compprehension.config.LtiRegistrationsProperties.RegistrationWithName;
import org.vstu.compprehension.models.businesslogic.lti.LtiDeepLinkingContext;

import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Сборка и подпись ответа LTI 1.3 Deep Linking ({@code LtiDeepLinkingResponse}) и
 * чтение уже добавленных в курс активностей через AGS line items (для дедупа).
 *
 * <p>Подпись и client_credentials-токен делаются тем же ключом регистрации, что и в
 * {@link org.vstu.compprehension.service.gradepassback.LtiAgsGradePassbackStrategy}.
 */
@Service
@Log4j2
public class DeepLinkingResponseService {

    private static final String CLAIM_MSG_TYPE      = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    private static final String CLAIM_VERSION       = "https://purl.imsglobal.org/spec/lti/claim/version";
    private static final String CLAIM_DEPLOYMENT_ID = "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
    private static final String CLAIM_CONTENT_ITEMS = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";
    private static final String CLAIM_DL_DATA       = "https://purl.imsglobal.org/spec/lti-dl/claim/data";
    private static final String LINEITEM_READONLY_SCOPE = "https://purl.imsglobal.org/spec/lti-ags/scope/lineitem.readonly";
    private static final String TAG_PREFIX = "compph_exercise_";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LtiRegistrationsProperties ltiRegistrations;

    public DeepLinkingResponseService(LtiRegistrationsProperties ltiRegistrations) {
        this.ltiRegistrations = ltiRegistrations;
    }

    /** Одно упражнение тренажёра, отдаваемое в Moodle как content item. */
    public record DeepLinkItem(long exerciseId, String title) {
    }

    /**
     * Строит и подписывает {@code LtiDeepLinkingResponse} с одним {@code ltiResourceLink}
     * на упражнение. URL у item не задаётся — Moodle подставит launch URL инструмента,
     * а {@code /lti/1_3/exercise} приоритетно читает custom-claim {@code exercise_id}.
     */
    public String buildSignedResponse(LtiDeepLinkingContext dl, List<DeepLinkItem> items) throws Exception {
        RegistrationWithName regWithName = requireRegistration(dl.platformIssuer());
        Registration reg = regWithName.registration();
        String kid = regWithName.name();

        List<Map<String, Object>> contentItems = new ArrayList<>(items.size());
        for (DeepLinkItem item : items) {
            String title = (item.title() != null && !item.title().isBlank())
                    ? item.title() : ("Exercise " + item.exerciseId());
            Map<String, Object> ci = new LinkedHashMap<>();
            ci.put("type", "ltiResourceLink");
            ci.put("title", title);
            ci.put("custom", Map.of("exercise_id", String.valueOf(item.exerciseId())));
            ci.put("lineItem", Map.of(
                    "scoreMaximum", 100,
                    "label", title,
                    "tag", TAG_PREFIX + item.exerciseId()));
            contentItems.add(ci);
        }

        Date now = new Date();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(reg.getClientId())
                .audience(dl.platformIssuer())
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 300_000))
                .jwtID(UUID.randomUUID().toString())
                .claim("nonce", UUID.randomUUID().toString())
                .claim("azp", reg.getClientId())
                .claim(CLAIM_DEPLOYMENT_ID, dl.deploymentId())
                .claim(CLAIM_MSG_TYPE, "LtiDeepLinkingResponse")
                .claim(CLAIM_VERSION, "1.3.0")
                .claim(CLAIM_CONTENT_ITEMS, contentItems);
        if (dl.data() != null) {
            claims.claim(CLAIM_DL_DATA, dl.data());
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(JOSEObjectType.JWT).build(),
                claims.build());
        jwt.sign(new RSASSASigner(loadPrivateKey(reg)));
        return jwt.serialize();
    }

    /**
     * exercise_id уже добавленных в курс активностей — по AGS line items (LTI-токен),
     * через {@code tag} вида {@code compph_exercise_<id>}. Fail-soft: при любой ошибке
     * (нет AGS, нет токена, ошибка сети) возвращает пустое множество.
     */
    public Set<Long> fetchExistingExerciseIds(LtiDeepLinkingContext dl) {
        if (dl.lineitemsUrl() == null || dl.lineitemsUrl().isBlank()) {
            return Set.of();
        }
        try {
            RegistrationWithName regWithName = requireRegistration(dl.platformIssuer());
            String tokenEndpoint = dl.platformIssuer() + "/mod/lti/token.php";
            String accessToken = obtainAccessToken(tokenEndpoint, regWithName.registration(),
                    regWithName.name(), LINEITEM_READONLY_SCOPE);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.ims.lis.v2.lineitemcontainer+json")));

            ResponseEntity<String> resp = restTemplate.exchange(
                    URI.create(dl.lineitemsUrl()), HttpMethod.GET, new HttpEntity<>(headers), String.class);

            Set<Long> result = new HashSet<>();
            JsonNode arr = objectMapper.readTree(resp.getBody());
            if (arr != null && arr.isArray()) {
                for (JsonNode lineitem : arr) {
                    String tag = lineitem.path("tag").asText("");
                    if (tag.startsWith(TAG_PREFIX)) {
                        try {
                            result.add(Long.parseLong(tag.substring(TAG_PREFIX.length())));
                        } catch (NumberFormatException ignored) {
                            // tag not in our format — skip
                        }
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("AGS line items dedup failed for {}: {}", dl.lineitemsUrl(), e.getMessage());
            return Set.of();
        }
    }

    private RegistrationWithName requireRegistration(String issuerUrl) {
        return ltiRegistrations.findByIssuerUrl(issuerUrl)
                .orElseThrow(() -> new IllegalStateException(
                        "LTI registration not configured for issuer " + issuerUrl
                                + " — добавьте compprehension.lti.registrations.<name>.* в env"));
    }

    private RSAPrivateKey loadPrivateKey(Registration reg) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(reg.getPrivateKeyPkcs8Base64());
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private String obtainAccessToken(String tokenEndpoint, Registration reg, String kid, String scope) throws Exception {
        Date now = new Date();
        JWTClaimsSet assertionClaims = new JWTClaimsSet.Builder()
                .issuer(reg.getClientId())
                .subject(reg.getClientId())
                .audience(tokenEndpoint)
                .issueTime(now)
                .expirationTime(new Date(now.getTime() + 60_000))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT assertion = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), assertionClaims);
        assertion.sign(new RSASSASigner(loadPrivateKey(reg)));

        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        form.add("client_assertion", assertion.serialize());
        form.add("scope", scope);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        ResponseEntity<String> resp = restTemplate.postForEntity(
                tokenEndpoint, new HttpEntity<>(form, headers), String.class);
        JsonNode token = objectMapper.readTree(resp.getBody());
        String accessToken = token.path("access_token").asText(null);
        if (accessToken == null) {
            throw new IllegalStateException("No access_token from " + tokenEndpoint
                    + " (error=" + token.path("error").asText("") + ")");
        }
        return accessToken;
    }
}
