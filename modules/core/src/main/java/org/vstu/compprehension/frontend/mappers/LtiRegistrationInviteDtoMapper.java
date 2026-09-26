package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiRegistrationInviteData;
import org.vstu.compprehension.frontend.dto.LtiRegistrationInviteDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class LtiRegistrationInviteDtoMapper implements Mapper<LtiRegistrationInviteData, LtiRegistrationInviteDto> {

    @Override
    public @NotNull LtiRegistrationInviteDto map(@NotNull LtiRegistrationInviteData source) {
        return new LtiRegistrationInviteDto(source.token(), source.expiresAt());
    }
}
