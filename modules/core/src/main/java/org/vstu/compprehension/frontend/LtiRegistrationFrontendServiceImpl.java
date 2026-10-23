package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.common.LmsUrlHelper;
import org.vstu.compprehension.common.RsaKeyHelper;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;
import org.vstu.compprehension.data.lti.LtiRegistrationInviteData;
import org.vstu.compprehension.data.lti.LtiToolConfigurationData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.enums.LtiPlatformKeyType;
import org.vstu.compprehension.frontend.dto.LtiRegistrationDto;
import org.vstu.compprehension.frontend.dto.LtiRegistrationInviteDto;
import org.vstu.compprehension.frontend.dto.LtiToolConfigurationDto;
import org.vstu.compprehension.frontend.dto.NewLtiRegistrationDto;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.services.LtiRegistrationDataService;
import org.vstu.compprehension.services.LtiToolConfigurationProvider;

import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LtiRegistrationFrontendServiceImpl implements LtiRegistrationFrontendService {
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final LtiRegistrationDataService ltiRegistrationService;
    private final Mapper<LtiRegistrationData, LtiRegistrationDto> registrationDtoMapper;
    private final Mapper<LtiRegistrationInviteData, LtiRegistrationInviteDto> inviteDtoMapper;
    private final LtiToolConfigurationProvider toolConfigurationProvider;
    private final Mapper<LtiToolConfigurationData, LtiToolConfigurationDto> toolConfigurationDtoMapper;

    @Override
    public @NotNull List<LtiRegistrationDto> getAll() {
        return registrationDtoMapper.mapAll(ltiRegistrationService.getAll());
    }

    @Override
    public @NotNull LtiRegistrationInviteDto createInvite(long createdByUserId, @Nullable String description) {
        return inviteDtoMapper.map(ltiRegistrationService.createInvite(createdByUserId, normalizeDescription(description)));
    }

    @Override
    public @NotNull LtiRegistrationDto registerManually(@NotNull NewLtiRegistrationDto registration) {
        String issuer = requireText(registration.issuer(), "issuer");
        String lmsUrl = LmsUrlHelper.toCanonicalLmsUrl(issuer);
        if (lmsUrl == null) {
            throw new IllegalArgumentException("Invalid LMS issuer " + issuer);
        }
        String deploymentId = registration.deploymentId();
        return registrationDtoMapper.map(ltiRegistrationService.registerManually(new NewLtiRegistrationData(
                lmsUrl,
                issuer,
                requireText(registration.clientId(), "clientId"),
                deploymentId == null || deploymentId.isBlank() ? null : deploymentId.trim(),
                requireText(registration.authorizationEndpoint(), "authorizationEndpoint"),
                requireText(registration.tokenEndpoint(), "tokenEndpoint"),
                toPlatformKey(registration.platformKeyType(), registration.platformKey())),
                normalizeDescription(registration.description())));
    }

    @Override
    public void deleteRegistration(long registrationId) {
        ltiRegistrationService.deleteRegistration(registrationId);
    }

    @Override
    public void updateDescription(long registrationId, @Nullable String description) {
        ltiRegistrationService.updateDescription(registrationId, normalizeDescription(description));
    }

    @Override
    public @NotNull LtiToolConfigurationDto getToolConfiguration() {
        return toolConfigurationDtoMapper.map(toolConfigurationProvider.getToolConfiguration());
    }

    private static @NotNull LtiPlatformKeyData toPlatformKey(@Nullable LtiPlatformKeyType type, @Nullable String value) {
        if (type == null) {
            throw new IllegalArgumentException("LTI registration platformKeyType is required");
        }
        String key = requireText(value, "platformKey");
        return switch (type) {
            case JWKS -> new LtiPlatformKeyData.Jwks(key);
            // Ключ вставляют и в PEM, и голым base64: храним одинаково.
            case PUBLIC_KEY -> new LtiPlatformKeyData.PublicKey(
                    Base64.getEncoder().encodeToString(RsaKeyHelper.parsePublicKey(key).getEncoded()));
        };
    }

    private static @Nullable String normalizeDescription(@Nullable String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        var trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("LTI registration description is longer than " + MAX_DESCRIPTION_LENGTH);
        }
        return trimmed;
    }

    private static @NotNull String requireText(@Nullable String value, @NotNull String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LTI registration " + field + " is required");
        }
        return value.trim();
    }
}
