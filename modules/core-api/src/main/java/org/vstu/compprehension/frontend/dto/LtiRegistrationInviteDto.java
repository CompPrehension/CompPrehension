package org.vstu.compprehension.frontend.dto;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record LtiRegistrationInviteDto(@NotNull String token, @NotNull Instant expiresAt) {
}
