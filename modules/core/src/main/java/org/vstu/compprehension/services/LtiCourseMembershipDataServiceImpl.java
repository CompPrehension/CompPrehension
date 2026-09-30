package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.repositories.data.LtiCourseMembershipDataRepository;

@Service
@RequiredArgsConstructor
class LtiCourseMembershipDataServiceImpl implements LtiCourseMembershipDataService {

    private final LtiCourseMembershipDataRepository memberships;

    @Transactional
    public void rememberSource(long courseId, @NotNull String issuer, @NotNull String clientId,
                               @NotNull String membershipsUrl) {
        memberships.rememberSource(courseId, issuer, clientId, membershipsUrl);
    }
}
