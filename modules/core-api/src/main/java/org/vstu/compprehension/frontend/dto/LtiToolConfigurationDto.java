package org.vstu.compprehension.frontend.dto;

import org.jetbrains.annotations.NotNull;

/**
 * Настройки CompPrehension, которые вводятся в LMS при ручном подключении инструмента.
 */
public record LtiToolConfigurationDto(
        @NotNull String launchUrl,
        @NotNull String loginUrl,
        @NotNull String jwksUrl,
        @NotNull String publicKeyPem) {
}
