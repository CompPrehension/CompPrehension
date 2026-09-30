package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiCourseMembershipSourceData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.LtiCourseMembershipRepository.LtiCourseMembershipSourceView;
import org.vstu.compprehension.utils.Strict;

@Component
class LtiCourseMembershipSourceMapper implements Mapper<LtiCourseMembershipSourceView, LtiCourseMembershipSourceData> {

    @Override
    public @NotNull LtiCourseMembershipSourceData map(@NotNull LtiCourseMembershipSourceView source) {
        long courseId = Strict.required(source.getCourseId(), "courseId", "lti course membership");
        String owner = "lti course membership of course " + courseId;
        return new LtiCourseMembershipSourceData(
                courseId,
                Strict.required(source.getEducationResourceId(), "educationResourceId", owner),
                Strict.required(source.getIssuer(), "issuer", owner),
                Strict.required(source.getClientId(), "clientId", owner),
                Strict.required(source.getMembershipsUrl(), "membershipsUrl", owner));
    }
}
