package org.vstu.compprehension.adapters.lti;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiToolConfigurationData;
import org.vstu.compprehension.services.LtiToolConfigurationProvider;

/**
 * CompPrehension как LTI-инструмент: его адреса и открытый ключ.
 */
@Component
public class LtiToolConfigurationService implements LtiToolConfigurationProvider {

    private final LtiToolProperties properties;
    private final LtiToolKeyProvider toolKeys;

    public LtiToolConfigurationService(LtiToolProperties properties, LtiToolKeyProvider toolKeys) {
        this.properties = properties;
        this.toolKeys = toolKeys;
    }

    public void ensureConfigured() {
        if (properties.getToolBaseUrl() == null || toolKeys.findKey().isEmpty()) {
            throw new IllegalStateException("LTI tool is not configured: set "
                    + "compprehension.lti.tool-base-url and compprehension.lti.tool-private-key-pkcs8-base64");
        }
    }

    public @NotNull String getBaseUrl() {
        ensureConfigured();
        return properties.getToolBaseUrl();
    }

    public @NotNull String getLaunchUrl() {
        return getBaseUrl() + "/lti/launch";
    }

    public @NotNull String getLoginUrl() {
        return getBaseUrl() + "/lti/login";
    }

    public @NotNull String getJwksUrl() {
        return getBaseUrl() + "/lti/jwks";
    }

    @Override
    public @NotNull LtiToolConfigurationData getToolConfiguration() {
        ensureConfigured();
        return new LtiToolConfigurationData(
                getLaunchUrl(),
                getLoginUrl(),
                getJwksUrl(),
                toolKeys.findKey().orElseThrow().getPublicPem());
    }
}
