package org.vstu.compprehension.moodle;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Per-Moodle конфигурация для WS-функций (`webservice-token`, `base-url`).
 * <p>Env-формат: {@code COMPPREHENSION_MOODLE_WS_REGISTRATIONS_<NAME>_<KEY>}.
 */
@Component
@ConfigurationProperties(prefix = "compprehension.moodle.ws")
@Setter
public class WsFuncMoodleConfig {
    @Getter
    private Map<String, Registration> registrations = new HashMap<>();

    private Map<String, RegistrationWithName> byBaseUrl = Map.of();

    public Optional<RegistrationWithName> findByBaseUrl(String baseUrl) {
        return Optional.ofNullable(byBaseUrl.get(baseUrl));
    }

    @PostConstruct
    void init() {
        var index = new HashMap<String, RegistrationWithName>();
        registrations.forEach((name, reg) -> {
            if (reg.getBaseUrl() == null || reg.getBaseUrl().isBlank()) {
                throw new IllegalStateException("WS-moodle '" + name + "': base-url is required");
            }
            if (reg.getWebserviceToken() == null || reg.getWebserviceToken().isBlank()) {
                throw new IllegalStateException("WS-moodle '" + name + "': webservice-token is required");
            }
            var prev = index.put(reg.getBaseUrl(), new RegistrationWithName(name, reg));
            if (prev != null) {
                throw new IllegalStateException("WS-moodle registrations have duplicate base-url: " + reg.getBaseUrl());
            }
        });
        byBaseUrl = Map.copyOf(index);
    }

    @Getter
    @Setter
    public static class Registration {
        private String baseUrl;
        private String webserviceToken;
    }

    public record RegistrationWithName(String name, Registration registration) {
    }
}
