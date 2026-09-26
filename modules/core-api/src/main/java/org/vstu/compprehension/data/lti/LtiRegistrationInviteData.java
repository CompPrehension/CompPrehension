package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record LtiRegistrationInviteData(@NotNull String token, @NotNull Instant expiresAt) {
}
