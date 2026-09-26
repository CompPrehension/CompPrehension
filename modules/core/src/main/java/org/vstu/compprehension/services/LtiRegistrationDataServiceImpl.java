package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.LtiRegistrationInviteData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class LtiRegistrationDataServiceImpl implements LtiRegistrationDataService {

    private static final Duration INVITE_TTL = Duration.ofHours(24);
    private static final int INVITE_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExternalSystemDataRepository externalSystems;

    @Transactional(readOnly = true)
    public @NotNull Optional<LtiRegistrationData> findByIssuer(@NotNull String issuer) {
        return externalSystems.findLtiRegistration(issuer);
    }

    @Transactional(readOnly = true)
    public @NotNull List<LtiRegistrationData> getAll() {
        return externalSystems.findAllLtiRegistrations();
    }

    @Transactional
    public @NotNull LtiRegistrationInviteData createInvite(long createdByUserId) {
        byte[] tokenBytes = new byte[INVITE_TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        var now = Instant.now();
        var expiresAt = now.plus(INVITE_TTL);
        externalSystems.createLtiRegistrationInvite(hash(token), createdByUserId, now, expiresAt);
        return new LtiRegistrationInviteData(token, expiresAt);
    }

    @Transactional(readOnly = true)
    public void ensureInviteUsable(@NotNull String inviteToken) {
        if (!externalSystems.isLtiRegistrationInviteUsable(hash(inviteToken), Instant.now())) {
            throw new SecurityException("LTI registration link is unknown, expired or already used");
        }
    }

    @Transactional
    public void deleteRegistration(long registrationId) {
        externalSystems.deleteLtiRegistration(registrationId);
    }

    @Transactional
    public @NotNull LtiRegistrationData registerByInvite(@NotNull String inviteToken,
                                                         @NotNull NewLtiRegistrationData registration) {
        var now = Instant.now();
        long inviteId = externalSystems.lockUsableLtiRegistrationInvite(hash(inviteToken), now)
                .orElseThrow(() -> new SecurityException("LTI registration link is unknown, expired or already used"));
        if (externalSystems.findLtiRegistration(registration.issuer()).isPresent()) {
            throw new IllegalStateException(String.format(
                    "LMS %s is already registered: delete the existing registration first", registration.issuer()));
        }

        // Ссылку выдал наш администратор — это и есть одобрение LMS, поэтому она становится доверенной.
        var educationResource = externalSystems.findEducationResource(registration.lmsUrl(), registration.lmsType())
                .orElseGet(() -> externalSystems.createEducationResourceIfAbsent(
                        registration.lmsUrl(), registration.lmsType(), EducationResourceTrustStatus.TRUSTED));
        if (educationResource.trustStatus() == EducationResourceTrustStatus.BANNED) {
            throw new SecurityException(String.format("LMS %s is banned", registration.lmsUrl()));
        }
        if (educationResource.trustStatus() == EducationResourceTrustStatus.UNTRUSTED) {
            educationResource = externalSystems.updateEducationResourceTrustStatus(
                    educationResource.id(), EducationResourceTrustStatus.TRUSTED);
        }
        if (externalSystems.existsLtiRegistration(educationResource.id())) {
            throw new IllegalStateException(String.format(
                    "LMS %s is already registered: delete the existing registration first", registration.lmsUrl()));
        }

        var created = externalSystems.createLtiRegistration(educationResource.id(), registration);
        externalSystems.markLtiRegistrationInviteUsed(inviteId, created.id(), now);
        return created;
    }

    private static @NotNull String hash(@NotNull String inviteToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(inviteToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
