package org.vstu.compprehension.service.lti;

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
import org.vstu.compprehension.config.LtiRegistrationsProperties;
import org.vstu.compprehension.config.LtiRegistrationsProperties.Registration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис проверки id_token'а LTI-запуска.
 */
@Service
public class LtiIdTokenVerifier {

    private final LtiRegistrationsProperties ltiRegistrations;
    private final Map<String, JwtDecoder> decodersByRegistrationName = new ConcurrentHashMap<>();

    public LtiIdTokenVerifier(LtiRegistrationsProperties ltiRegistrations) {
        this.ltiRegistrations = ltiRegistrations;
    }

    public @NotNull Jwt verify(@NotNull String rawIdToken) {
        // До проверки подписи iss нужен только для выбора регистрации, чьим ключом проверять.
        var issuer = readUnverifiedIssuer(rawIdToken);
        var registration = ltiRegistrations.findByIssuerUrl(issuer)
                .orElseThrow(() -> new SecurityException(String.format("LTI issuer %s is not registered", issuer)));
        var decoder = decodersByRegistrationName.computeIfAbsent(
                registration.name(), name -> createDecoder(registration.registration()));
        try {
            return decoder.decode(rawIdToken);
        } catch (JwtException ex) {
            throw new SecurityException("Invalid LTI id_token: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String readUnverifiedIssuer(@NotNull String rawIdToken) {
        String issuer;
        try {
            issuer = JWTParser.parse(rawIdToken).getJWTClaimsSet().getIssuer();
        } catch (ParseException ex) {
            throw new SecurityException("Malformed LTI id_token", ex);
        }
        if (issuer == null) {
            throw new SecurityException("LTI id_token has no issuer");
        }
        return issuer;
    }

    private static @NotNull JwtDecoder createDecoder(@NotNull Registration registration) {
        var decoder = registration.getPlatformPublicKeyBase64() != null
                ? NimbusJwtDecoder.withPublicKey(loadPublicKey(registration.getPlatformPublicKeyBase64()))
                        .signatureAlgorithm(SignatureAlgorithm.RS256)
                        .build()
                : NimbusJwtDecoder.withJwkSetUri(registration.getPlatformJwksUrl())
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(registration.getIssuerUrl()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        audience -> audience != null && audience.contains(registration.getClientId()))));
        return decoder;
    }

    private static @NotNull RSAPublicKey loadPublicKey(@NotNull String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid LTI platform public key", ex);
        }
    }
}
