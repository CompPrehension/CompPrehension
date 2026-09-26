package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.enums.EducationResourceType;

public record NewLtiRegistrationData(
        @NotNull String lmsUrl,
        @NotNull EducationResourceType lmsType,
        @NotNull String issuer,
        @NotNull String clientId,
        @Nullable String deploymentId,
        @NotNull String authorizationEndpoint,
        @NotNull String tokenEndpoint,
        @NotNull String jwksUri) {
}
