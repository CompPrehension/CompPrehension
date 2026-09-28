package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class LtiRegistrationMapper implements Mapper<LtiRegistrationEntity, LtiRegistrationData> {

    @Override
    public @NotNull LtiRegistrationData map(@NotNull LtiRegistrationEntity source) {
        long id = Strict.required(source.getId(), "id", "lti registration " + source.getIssuer());
        String owner = "lti registration " + id;
        var educationResource = Strict.required(source.getEducationResource(), "educationResource", owner);
        return new LtiRegistrationData(
                id,
                Strict.required(educationResource.getId(), "educationResource.id", owner),
                Strict.required(educationResource.getUrl(), "educationResource.url", owner),
                Strict.required(source.getIssuer(), "issuer", owner),
                Strict.required(source.getClientId(), "clientId", owner),
                source.getDescription(),
                source.getDeploymentId(),
                Strict.required(source.getMethod(), "method", owner),
                Strict.required(source.getAuthorizationEndpoint(), "authorizationEndpoint", owner),
                Strict.required(source.getTokenEndpoint(), "tokenEndpoint", owner),
                toPlatformKey(source, owner),
                Strict.required(source.getCreatedAt(), "createdAt", owner));
    }

    private @NotNull LtiPlatformKeyData toPlatformKey(@NotNull LtiRegistrationEntity source, @NotNull String owner) {
        return source.getPlatformPublicKey() != null
                ? new LtiPlatformKeyData.PublicKey(source.getPlatformPublicKey())
                : new LtiPlatformKeyData.Jwks(Strict.required(source.getJwksUri(), "jwksUri", owner));
    }
}
