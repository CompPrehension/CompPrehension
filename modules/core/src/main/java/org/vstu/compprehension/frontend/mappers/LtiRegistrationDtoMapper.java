package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.frontend.dto.LtiRegistrationDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class LtiRegistrationDtoMapper implements Mapper<LtiRegistrationData, LtiRegistrationDto> {

    @Override
    public @NotNull LtiRegistrationDto map(@NotNull LtiRegistrationData source) {
        return new LtiRegistrationDto(
                source.id(),
                source.educationResourceUrl(),
                source.issuer(),
                source.clientId(),
                source.createdAt());
    }
}
