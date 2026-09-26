package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.frontend.dto.LtiRegistrationDto;
import org.vstu.compprehension.frontend.dto.LtiRegistrationInviteDto;

import java.util.List;

public interface LtiRegistrationFrontendService {
    @NotNull List<LtiRegistrationDto> getAll();

    @NotNull LtiRegistrationInviteDto createInvite(long createdByUserId);

    void deleteRegistration(long registrationId);
}
