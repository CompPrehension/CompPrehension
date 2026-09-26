package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.LtiRegistrationInviteData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;

import java.util.List;
import java.util.Optional;

public interface LtiRegistrationDataService {
    @NotNull Optional<LtiRegistrationData> findByIssuer(@NotNull String issuer);

    @NotNull List<LtiRegistrationData> getAll();

    @NotNull LtiRegistrationInviteData createInvite(long createdByUserId);

    void ensureInviteUsable(@NotNull String inviteToken);

    void deleteRegistration(long registrationId);

    @NotNull LtiRegistrationData registerByInvite(@NotNull String inviteToken, @NotNull NewLtiRegistrationData registration);
}
