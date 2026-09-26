package org.vstu.compprehension.frontend.dto;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record LtiRegistrationDto(
        long id,
        @NotNull String lmsUrl,
        @NotNull String issuer,
        @NotNull String clientId,
        @NotNull Instant createdAt) {
}
