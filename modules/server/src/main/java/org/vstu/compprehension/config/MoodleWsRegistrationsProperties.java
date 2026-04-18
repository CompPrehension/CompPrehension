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
 * Moodle Web Services регистрации (base URL + admin-токен). Значения берутся из env с префиксом
 * {@code COMPPREHENSION_MOODLE_WS_REGISTRATIONS_<NAME>_<KEY>}, где {@code <KEY>} -
 * {@code BASE_URL} или {@code WEBSERVICE_TOKEN}. Ключ внешней Map - логическое имя (env-ключи
 * не допускают спецсимволы URL, поэтому baseUrl не может быть ключом напрямую); на старте
 * строится обратный индекс {@link #byBaseUrl} для O(1) lookup по baseUrl.
 *
 * <p>Используется только при {@code compprehension.grade-passback.moodle-ws.enabled=true};
 * но сам бин properties создаётся всегда (с пустой мапой, если ничего не задано).
 */
@Component
@ConfigurationProperties(prefix = "compprehension.moodle-ws")
@Setter
public class MoodleWsRegistrationsProperties {
    private Map<String, Registration> registrations = new HashMap<>();

    private Map<String, Registration> byBaseUrl = Map.of();

    public Optional<Registration> findByBaseUrl(String baseUrl) {
        return Optional.ofNullable(byBaseUrl.get(baseUrl));
    }

    @PostConstruct
    void init() {
        var index = new HashMap<String, Registration>();
        registrations.forEach((name, reg) -> {
            if (reg.getBaseUrl() == null || reg.getBaseUrl().isBlank()) {
                throw new IllegalStateException("Moodle WS registration '" + name + "': base-url is required");
            }
            if (reg.getWebserviceToken() == null || reg.getWebserviceToken().isBlank()) {
                throw new IllegalStateException("Moodle WS registration '" + name + "': webservice-token is required");
            }
            var prev = index.put(reg.getBaseUrl(), reg);
            if (prev != null) {
                throw new IllegalStateException("Moodle WS registrations have duplicate base-url: " + reg.getBaseUrl());
            }
        });
        byBaseUrl = Map.copyOf(index);
    }

    @Getter
    @Setter
    public static class Registration {
        /** Base URL Moodle-инсталляции, например {@code http://localhost:8081}. */
        private String baseUrl;
        /** Admin WS token с правами на нужные WS-функции. */
        private String webserviceToken;
    }
}
