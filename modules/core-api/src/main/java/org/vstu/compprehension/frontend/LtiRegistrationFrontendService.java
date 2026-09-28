package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.frontend.dto.LtiRegistrationDto;
import org.vstu.compprehension.frontend.dto.LtiRegistrationInviteDto;
import org.vstu.compprehension.frontend.dto.LtiToolConfigurationDto;
import org.vstu.compprehension.frontend.dto.NewLtiRegistrationDto;

import java.util.List;

public interface LtiRegistrationFrontendService {
    @NotNull List<LtiRegistrationDto> getAll();

    @NotNull LtiToolConfigurationDto getToolConfiguration();

    @NotNull LtiRegistrationInviteDto createInvite(long createdByUserId, @Nullable String description);

    @NotNull LtiRegistrationDto registerManually(@NotNull NewLtiRegistrationDto registration);

    void deleteRegistration(long registrationId);

    void updateDescription(long registrationId, @Nullable String description);
}
