package org.vstu.compprehension.frontend.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.LtiRegistrationMethod;

import java.time.Instant;

public record LtiRegistrationDto(
        long id,
        @NotNull String lmsUrl,
        @NotNull String issuer,
        @NotNull String clientId,
        @Nullable String description,
        @NotNull LtiRegistrationMethod method,
        @NotNull Instant createdAt) {
}
