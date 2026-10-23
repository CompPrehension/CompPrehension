package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;

/**
 * Настройки CompPrehension, которые вводятся в LMS при ручном подключении инструмента.
 */
public record LtiToolConfigurationData(
        @NotNull String launchUrl,
        @NotNull String loginUrl,
        @NotNull String jwksUrl,
        @NotNull String publicKeyPem) {
}
