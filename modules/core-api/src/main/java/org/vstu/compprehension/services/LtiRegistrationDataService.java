package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.LtiRegistrationInviteData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;

import java.util.List;
import java.util.Optional;

public interface LtiRegistrationDataService {
    @NotNull Optional<LtiRegistrationData> findByIssuerAndClientId(@NotNull String issuer, @NotNull String clientId);

    @NotNull List<LtiRegistrationData> findAllByIssuer(@NotNull String issuer);

    @NotNull List<LtiRegistrationData> getAll();

    @NotNull LtiRegistrationInviteData createInvite(long createdByUserId, @Nullable String description);

    void ensureInviteUsable(@NotNull String inviteToken);

    void deleteRegistration(long registrationId);

    void updateDescription(long registrationId, @Nullable String description);

    @NotNull LtiRegistrationData registerByInvite(@NotNull String inviteToken, @NotNull NewLtiRegistrationData registration);

    @NotNull LtiRegistrationData registerManually(@NotNull NewLtiRegistrationData registration, @Nullable String description);
}
