package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище начатых в этой сессии OIDC-логинов LTI: {@code state} → {@code nonce}.
 */
@Component
@SessionScope
public class LtiPendingLogins implements Serializable {

    private final ConcurrentHashMap<String, String> noncesByState = new ConcurrentHashMap<>();

    public void savePendingLogin(@NotNull String state, @NotNull String nonce) {
        noncesByState.put(state, nonce);
    }

    public @Nullable String takeNonce(@NotNull String state) {
        return noncesByState.remove(state);
    }
}
