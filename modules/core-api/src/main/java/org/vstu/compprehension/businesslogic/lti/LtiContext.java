package org.vstu.compprehension.businesslogic.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LtiContext(
        @Nullable String lineitemUrl,
        @NotNull String issuer,
        @NotNull String clientId,
        long educationResourceId,
        @Nullable LtiCourseContext course,
        @Nullable Long exerciseId,
        @Nullable String membershipsUrl
) {
}
