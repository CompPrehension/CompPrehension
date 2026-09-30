package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;

public interface LtiCourseMembershipDataService {
    void rememberSource(long courseId, @NotNull String issuer, @NotNull String clientId, @NotNull String membershipsUrl);
}
