package org.vstu.compprehension.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LTI-регистрации (client_id + приватный ключ + issuer URL) для каждой подключённой LMS.
 * Значения берутся из env с префиксом {@code COMPPREHENSION_LTI_REGISTRATIONS_<NAME>_<KEY>}.
 * Ключ внешней Map- логическое имя, оно же используется как {@code kid} в JWKS и в
 * client_assertion JWT при grade passback, поэтому имена должны быть уникальны.
 *
 * <p>Env-ключи не допускают спецсимволы URL, поэтому issuerUrl не может быть ключом Map
 * напрямую; на старте строится обратный индекс {@link #byIssuerUrl} для O(1) lookup.
 */
@Component
@ConfigurationProperties(prefix = "compprehension.lti")
@Setter
public class LtiRegistrationsProperties {
    @Getter
    private Map<String, Registration> registrations = new HashMap<>();

    private Map<String, RegistrationWithName> byIssuerUrl = Map.of();

    public Optional<RegistrationWithName> findByIssuerUrl(String issuerUrl) {
        return Optional.ofNullable(byIssuerUrl.get(issuerUrl));
    }

    @PostConstruct
    void init() {
        var index = new HashMap<String, RegistrationWithName>();
        registrations.forEach((name, reg) -> {
            if (reg.getIssuerUrl() == null || reg.getIssuerUrl().isBlank()) {
                throw new IllegalStateException("LTI registration '" + name + "': issuer-url is required");
            }
            if (reg.getClientId() == null || reg.getClientId().isBlank()) {
                throw new IllegalStateException("LTI registration '" + name + "': client-id is required");
            }
            if (reg.getPrivateKeyPkcs8Base64() == null || reg.getPrivateKeyPkcs8Base64().isBlank()) {
                throw new IllegalStateException("LTI registration '" + name + "': private-key-pkcs8-base64 is required");
            }
            var prev = index.put(reg.getIssuerUrl(), new RegistrationWithName(name, reg));
            if (prev != null) {
                throw new IllegalStateException("LTI registrations have duplicate issuer-url: " + reg.getIssuerUrl());
            }
        });
        byIssuerUrl = Map.copyOf(index);
    }

    @Getter
    @Setter
    public static class Registration {
        /** Issuer URL LMS; для Moodle — base URL инсталляции (из JWT claim {@code iss}). */
        private String issuerUrl;
        /** client_id, выданный LMS при регистрации External Tool. */
        private String clientId;
        /** RSA private key (PKCS8 DER) в base64; парная public key публикуется в JWKS. */
        private String privateKeyPkcs8Base64;
    }

    /** Пара (имя регистрации, её настройки) — удобный возврат из find-методов. */
    public record RegistrationWithName(String name, Registration registration) {
    }
}
