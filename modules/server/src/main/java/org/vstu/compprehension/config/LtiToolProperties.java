package org.vstu.compprehension.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки CompPrehension как инструмента LTI.
 */
@Component
@ConfigurationProperties(prefix = "compprehension.lti")
@Getter
@Setter
public class LtiToolProperties {
    /** {@code kid} ключа инструмента в JWKS. */
    public static final String TOOL_KEY_ID = "tool";

    /** Ключ инструмента (PKCS8 DER в base64): им подписываются запросы и ответы для всех подключённых LMS. */
    private String toolPrivateKeyPkcs8Base64;

    /**
     * Публичный адрес инструмента без {@code /} на конце, например {@code https://dev.compprehension.ru}: из него
     * строятся адреса, которые сообщаются LMS при динамической регистрации.
     */
    private String toolBaseUrl;
}
