package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;

/**
 * Ключ, которым LMS подписывает запуски инструмента.
 */
public sealed interface LtiPlatformKeyData {

    /** LMS публикует свои ключи по адресу JWKS. */
    record Jwks(@NotNull String url) implements LtiPlatformKeyData {
    }

    /** Открытый ключ RSA LMS в X.509 DER, base64. */
    record PublicKey(@NotNull String x509Base64) implements LtiPlatformKeyData {
    }
}
