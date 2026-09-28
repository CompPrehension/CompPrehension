package org.vstu.compprehension.service.lti;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.common.RsaKeyHelper;
import org.vstu.compprehension.config.LtiToolProperties;
import org.vstu.compprehension.data.lti.LtiToolConfigurationData;
import org.vstu.compprehension.services.LtiToolConfigurationProvider;

import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

/**
 * CompPrehension как LTI-инструмент: его адреса и открытый ключ.
 */
@Service
public class LtiToolConfigurationService implements LtiToolConfigurationProvider {

    private final LtiToolProperties properties;

    public LtiToolConfigurationService(LtiToolProperties properties) {
        this.properties = properties;
    }

    public void ensureConfigured() {
        if (properties.getToolBaseUrl() == null || properties.getToolPrivateKeyPkcs8Base64() == null) {
            throw new IllegalStateException("LTI tool is not configured: set "
                    + "compprehension.lti.tool-base-url and compprehension.lti.tool-private-key-pkcs8-base64");
        }
    }

    public @NotNull String getBaseUrl() {
        ensureConfigured();
        return properties.getToolBaseUrl();
    }

    public @NotNull String getLaunchUrl() {
        return getBaseUrl() + "/lti/1_3/launch";
    }

    public @NotNull String getLoginUrl() {
        return getBaseUrl() + "/lti/1_3/login";
    }

    public @NotNull String getJwksUrl() {
        return getBaseUrl() + "/lti/1_3/jwks";
    }

    public @NotNull Optional<RSAPublicKey> findPublicKey() {
        return Optional.ofNullable(properties.getToolPrivateKeyPkcs8Base64()).map(RsaKeyHelper::derivePublicKey);
    }

    @Override
    public @NotNull LtiToolConfigurationData getToolConfiguration() {
        ensureConfigured();
        return new LtiToolConfigurationData(
                getLaunchUrl(),
                getLoginUrl(),
                getJwksUrl(),
                RsaKeyHelper.toPem(findPublicKey().orElseThrow()));
    }
}
