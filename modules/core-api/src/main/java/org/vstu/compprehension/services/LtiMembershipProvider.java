package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.lti.LtiCourseMemberData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;

import java.util.List;

/**
 * Out-port: участники курса LMS.
 */
public interface LtiMembershipProvider {
    /**
     * Весь список участников — или исключение: по неполному списку роли сверять нельзя, иначе отнимутся лишние.
     */
    @NotNull List<LtiCourseMemberData> fetchMembers(@NotNull LtiRegistrationData tool, @NotNull String membershipsUrl);
}
