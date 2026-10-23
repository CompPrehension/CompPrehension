package org.vstu.compprehension.adapters.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.common.RsaKeyHelper;

import java.util.Optional;

/**
 * Ключ LTI-инструмента из настроек сервера, если он задан.
 */
@Component
public class LtiToolKeyProvider {

    private final @Nullable LtiToolKey key;

    public LtiToolKeyProvider(@NotNull LtiToolProperties properties) {
        String pkcs8Base64 = properties.getToolPrivateKeyPkcs8Base64();
        this.key = pkcs8Base64 == null ? null : new LtiToolKey(RsaKeyHelper.parsePrivateKey(pkcs8Base64));
    }

    public @NotNull Optional<LtiToolKey> findKey() {
        return Optional.ofNullable(key);
    }
}
