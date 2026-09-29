package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiToolConfigurationData;
import org.vstu.compprehension.frontend.dto.LtiToolConfigurationDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class LtiToolConfigurationDtoMapper implements Mapper<LtiToolConfigurationData, LtiToolConfigurationDto> {

    @Override
    public @NotNull LtiToolConfigurationDto map(@NotNull LtiToolConfigurationData source) {
        return new LtiToolConfigurationDto(
                source.launchUrl(),
                source.loginUrl(),
                source.jwksUrl(),
                source.publicKeyPem());
    }
}
