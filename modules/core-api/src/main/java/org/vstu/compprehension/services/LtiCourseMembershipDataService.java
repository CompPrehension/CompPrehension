package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;

public interface LtiCourseMembershipDataService {
    void saveLtiCourseMembership(long courseId, @NotNull String issuer, @NotNull String clientId, @NotNull String membershipsUrl);
}
