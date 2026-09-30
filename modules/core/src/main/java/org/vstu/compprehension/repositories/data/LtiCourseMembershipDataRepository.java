package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.lti.LtiCourseMembershipSourceData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.LtiCourseMembershipRepository;
import org.vstu.compprehension.repositories.entity.LtiCourseMembershipRepository.LtiCourseMembershipSourceView;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LtiCourseMembershipDataRepository {

    private final LtiCourseMembershipRepository repository;
    private final Mapper<LtiCourseMembershipSourceView, LtiCourseMembershipSourceData> sourceMapper;

    @Transactional
    public void saveLtiCourseMembership(long courseId, @NotNull String issuer, @NotNull String clientId,
                                        @NotNull String membershipsUrl) {
        repository.upsert(courseId, issuer, clientId, membershipsUrl);
    }

    @Transactional(readOnly = true)
    public @NotNull List<LtiCourseMembershipSourceData> findSourcesOfTrustedLms() {
        return sourceMapper.mapAll(repository.findSourcesOfTrustedLms());
    }
}
