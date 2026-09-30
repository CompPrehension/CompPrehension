package org.vstu.compprehension.adapters.lti;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.common.RsaKeyHelper;

import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Ключ CompPrehension как LTI-инструмента: им подписываются JWT для LMS.
 */
public final class LtiToolKey {
    private static final String KEY_ID = "tool";
    private static final JWSHeader HEADER = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(KEY_ID)
            .type(JOSEObjectType.JWT)
            .build();

    private final @NotNull RSASSASigner signer;
    private final @NotNull String publicJwks;
    private final @NotNull String publicPem;

    LtiToolKey(@NotNull RSAPrivateCrtKey privateKey) {
        RSAPublicKey publicKey = RsaKeyHelper.derivePublicKey(privateKey);
        var key = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(KEY_ID)
                .build();
        try {
            this.signer = new RSASSASigner(key);
        } catch (JOSEException ex) {
            throw new IllegalArgumentException("LTI tool key cannot sign RS256", ex);
        }
        this.publicJwks = new JWKSet(key.toPublicJWK()).toString();
        this.publicPem = RsaKeyHelper.toPem(publicKey);
    }

    public @NotNull String sign(@NotNull JWTClaimsSet claims) {
        var jwt = new SignedJWT(HEADER, claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException ex) {
            throw new IllegalStateException("Could not sign JWT with LTI tool key", ex);
        }
        return jwt.serialize();
    }

    public @NotNull String getPublicJwks() {
        return publicJwks;
    }

    public @NotNull String getPublicPem() {
        return publicPem;
    }
}
