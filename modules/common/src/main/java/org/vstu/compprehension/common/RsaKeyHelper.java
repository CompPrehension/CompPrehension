package org.vstu.compprehension.common;

import org.jetbrains.annotations.NotNull;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

public final class RsaKeyHelper {
    private static final Pattern PEM_ARMOR = Pattern.compile("-----(BEGIN|END) PUBLIC KEY-----|\\s");
    private static final Base64.Encoder PEM_BODY = Base64.getMimeEncoder(64, new byte[]{'\n'});

    private RsaKeyHelper() {
    }

    /** Открытая часть ключа RSA, заданного в PKCS8 DER в base64. */
    public static @NotNull RSAPublicKey derivePublicKey(@NotNull String privateKeyPkcs8Base64) {
        try {
            var rsa = KeyFactory.getInstance("RSA");
            var privateKey = (RSAPrivateCrtKey) rsa.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPkcs8Base64)));
            return (RSAPublicKey) rsa.generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException ex) {
            throw new IllegalStateException("Invalid RSA private key", ex);
        }
    }

    /** Открытый ключ RSA в PEM или в base64 X.509 DER без обрамления. */
    public static @NotNull RSAPublicKey parsePublicKey(@NotNull String pemOrBase64) {
        try {
            byte[] der = Base64.getDecoder().decode(PEM_ARMOR.matcher(pemOrBase64).replaceAll(""));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException | ClassCastException ex) {
            throw new IllegalArgumentException("Not an RSA public key in PEM or X.509 DER base64", ex);
        }
    }

    public static @NotNull String toPem(@NotNull RSAPublicKey key) {
        return "-----BEGIN PUBLIC KEY-----\n" + PEM_BODY.encodeToString(key.getEncoded()) + "\n-----END PUBLIC KEY-----\n";
    }
}
