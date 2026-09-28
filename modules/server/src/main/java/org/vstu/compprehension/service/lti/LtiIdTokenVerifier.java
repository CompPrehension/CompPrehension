package org.vstu.compprehension.service.lti;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.common.RsaKeyHelper;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис проверки id_token'а LTI-запуска.
 */
@Service
public class LtiIdTokenVerifier {

    private final LtiRegistrationRegistry ltiRegistrations;
    private final Map<LtiPlatform, JwtDecoder> decodersByPlatform = new ConcurrentHashMap<>();

    public LtiIdTokenVerifier(LtiRegistrationRegistry ltiRegistrations) {
        this.ltiRegistrations = ltiRegistrations;
    }

    public @NotNull VerifiedLtiIdToken verify(@NotNull String rawIdToken) {
        // До проверки подписи iss и client_id нужны только для выбора регистрации, чьим ключом проверять.
        var claims = readUnverifiedClaims(rawIdToken);
        var issuer = claims.getIssuer();
        if (issuer == null) {
            throw new SecurityException("LTI id_token has no issuer");
        }
        var clientId = readClientId(claims);
        var platform = ltiRegistrations.findByIssuerAndClientId(issuer, clientId)
                .orElseThrow(() -> new SecurityException(String.format(
                        "LTI tool with client_id %s is not registered for issuer %s", clientId, issuer)));
        var decoder = decodersByPlatform.computeIfAbsent(platform, LtiIdTokenVerifier::createDecoder);
        try {
            return new VerifiedLtiIdToken(decoder.decode(rawIdToken), platform);
        } catch (JwtException ex) {
            throw new SecurityException("Invalid LTI id_token: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull JWTClaimsSet readUnverifiedClaims(@NotNull String rawIdToken) {
        try {
            return JWTParser.parse(rawIdToken).getJWTClaimsSet();
        } catch (ParseException ex) {
            throw new SecurityException("Malformed LTI id_token", ex);
        }
    }

    /** По OpenID Connect при нескольких адресатах токена client_id указывается в azp. */
    private static @NotNull String readClientId(@NotNull JWTClaimsSet claims) {
        Object authorizedParty = claims.getClaim("azp");
        if (authorizedParty != null) {
            return authorizedParty.toString();
        }
        var audience = claims.getAudience();
        if (audience.size() != 1) {
            throw new SecurityException("LTI id_token must name the tool in azp or in a single aud");
        }
        return audience.getFirst();
    }

    private static @NotNull JwtDecoder createDecoder(@NotNull LtiPlatform platform) {
        var decoder = switch (platform.platformKey()) {
            case LtiPlatformKeyData.Jwks jwks -> NimbusJwtDecoder.withJwkSetUri(jwks.url())
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build();
            case LtiPlatformKeyData.PublicKey key -> NimbusJwtDecoder.withPublicKey(RsaKeyHelper.parsePublicKey(key.x509Base64()))
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(platform.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        audience -> audience != null && audience.contains(platform.clientId()))));
        return decoder;
    }
}
