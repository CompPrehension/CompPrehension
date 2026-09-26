package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.LtiRegistrationInviteData;
import org.vstu.compprehension.frontend.dto.LtiRegistrationDto;
import org.vstu.compprehension.frontend.dto.LtiRegistrationInviteDto;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.services.LtiRegistrationDataService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LtiRegistrationFrontendServiceImpl implements LtiRegistrationFrontendService {
    private final LtiRegistrationDataService ltiRegistrationService;
    private final Mapper<LtiRegistrationData, LtiRegistrationDto> registrationDtoMapper;
    private final Mapper<LtiRegistrationInviteData, LtiRegistrationInviteDto> inviteDtoMapper;

    @Override
    public @NotNull List<LtiRegistrationDto> getAll() {
        return registrationDtoMapper.mapAll(ltiRegistrationService.getAll());
    }

    @Override
    public @NotNull LtiRegistrationInviteDto createInvite(long createdByUserId) {
        return inviteDtoMapper.map(ltiRegistrationService.createInvite(createdByUserId));
    }

    @Override
    public void deleteRegistration(long registrationId) {
        ltiRegistrationService.deleteRegistration(registrationId);
    }
}
