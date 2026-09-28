package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.data.lti.LtiPlatformKeyData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.data.user.EducationResourceUserData;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;
import org.vstu.compprehension.entities.external_system.LtiRegistrationInviteEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.enums.LtiRegistrationMethod;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceUserRepository;
import org.vstu.compprehension.repositories.entity.EducationResourceUserRepository.EducationResourceUserView;
import org.vstu.compprehension.repositories.entity.LtiRegistrationInviteRepository;
import org.vstu.compprehension.repositories.entity.LtiRegistrationRepository;
import org.vstu.compprehension.repositories.entity.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExternalSystemDataRepository {

    private final EducationResourceRepository educationResourceRepository;
    private final EducationResourceUserRepository educationResourceUserRepository;
    private final LtiRegistrationRepository ltiRegistrationRepository;
    private final LtiRegistrationInviteRepository ltiRegistrationInviteRepository;
    private final UserRepository userRepository;
    private final Mapper<EducationResourceEntity, EducationResourceData> educationResourceMapper;
    private final Mapper<EducationResourceUserView, EducationResourceUserData> educationResourceUserMapper;
    private final Mapper<LtiRegistrationEntity, LtiRegistrationData> ltiRegistrationMapper;

    @Transactional(readOnly = true)
    public @NotNull Optional<EducationResourceData> findEducationResource(
            @NotNull String url, @NotNull EducationResourceType type) {
        return educationResourceRepository.findByUrlAndType(url, type)
                .map(educationResourceMapper::map);
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<EducationResourceData> findEducationResource(long id) {
        return educationResourceRepository.findById(id).map(educationResourceMapper::map);
    }

    @Transactional(readOnly = true)
    public @NotNull List<EducationResourceData> findEducationResources(
            @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        return educationResourceMapper.mapAll(
                educationResourceRepository.findByTypeAndTrustStatus(type, trustStatus));
    }

    @Transactional(readOnly = true)
    public @NotNull List<EducationResourceUserData> findEducationResourceUsers(long educationResourceId) {
        return educationResourceUserMapper.mapAll(
                educationResourceUserRepository.findUsersByEducationResourceId(educationResourceId));
    }

    @Transactional
    public @NotNull EducationResourceData createEducationResourceIfAbsent(
            @NotNull String url, @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        educationResourceRepository.createIfAbsent(url, type.name(), trustStatus.name());
        return findEducationResource(url, type)
                .orElseThrow(() -> new IllegalStateException(
                        "Education resource " + type + " " + url + " not found after insert"));
    }

    @Transactional
    public @NotNull EducationResourceData updateEducationResourceTrustStatus(
            long educationResourceId, @NotNull EducationResourceTrustStatus trustStatus) {
        if (educationResourceRepository.updateTrustStatus(educationResourceId, trustStatus) != 1) {
            throw new IllegalStateException("Education resource " + educationResourceId + " not found");
        }
        return educationResourceRepository.findById(educationResourceId)
                .map(educationResourceMapper::map)
                .orElseThrow(() -> new IllegalStateException("Education resource " + educationResourceId + " not found after update"));
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<LtiRegistrationData> findLtiRegistration(@NotNull String issuer, @NotNull String clientId) {
        return ltiRegistrationRepository.findByIssuerAndClientId(issuer, clientId).map(ltiRegistrationMapper::map);
    }

    @Transactional(readOnly = true)
    public @NotNull List<LtiRegistrationData> findLtiRegistrations(@NotNull String issuer) {
        return ltiRegistrationMapper.mapAll(ltiRegistrationRepository.findAllByIssuer(issuer));
    }

    @Transactional(readOnly = true)
    public @NotNull List<LtiRegistrationData> findAllLtiRegistrations() {
        return ltiRegistrationMapper.mapAll(ltiRegistrationRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional
    public @NotNull LtiRegistrationData createLtiRegistration(long educationResourceId,
                                                              @NotNull NewLtiRegistrationData registration,
                                                              @Nullable String description,
                                                              @NotNull LtiRegistrationMethod method) {
        var entity = new LtiRegistrationEntity();
        entity.setEducationResource(educationResourceRepository.getReferenceById(educationResourceId));
        entity.setIssuer(registration.issuer());
        entity.setClientId(registration.clientId());
        entity.setDescription(description);
        entity.setDeploymentId(registration.deploymentId());
        entity.setMethod(method);
        entity.setAuthorizationEndpoint(registration.authorizationEndpoint());
        entity.setTokenEndpoint(registration.tokenEndpoint());
        switch (registration.platformKey()) {
            case LtiPlatformKeyData.Jwks jwks -> entity.setJwksUri(jwks.url());
            case LtiPlatformKeyData.PublicKey key -> entity.setPlatformPublicKey(key.x509Base64());
        }
        ltiRegistrationRepository.saveAndFlush(entity);
        return findLtiRegistration(registration.issuer(), registration.clientId())
                .orElseThrow(() -> new IllegalStateException("LTI registration " + registration.issuer() + " "
                        + registration.clientId() + " not found after insert"));
    }

    /** Использованные ссылки остаются использованными, теряя только связь с удалённой регистрацией. */
    @Transactional
    public void deleteLtiRegistration(long registrationId) {
        if (!ltiRegistrationRepository.existsById(registrationId)) {
            throw new NoSuchElementException("LTI registration " + registrationId + " not found");
        }
        ltiRegistrationInviteRepository.detachFromRegistration(registrationId);
        ltiRegistrationRepository.deleteById(registrationId);
    }

    @Transactional
    public void createLtiRegistrationInvite(@NotNull String tokenHash, @Nullable String description, long createdByUserId,
                                            @NotNull Instant createdAt, @NotNull Instant expiresAt) {
        var entity = new LtiRegistrationInviteEntity();
        entity.setTokenHash(tokenHash);
        entity.setDescription(description);
        entity.setCreatedBy(userRepository.getReferenceById(createdByUserId));
        entity.setCreatedAt(createdAt);
        entity.setExpiresAt(expiresAt);
        ltiRegistrationInviteRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public boolean isLtiRegistrationInviteUsable(@NotNull String tokenHash, @NotNull Instant now) {
        return ltiRegistrationInviteRepository.findByTokenHash(tokenHash)
                .filter(invite -> isUsable(invite, now))
                .isPresent();
    }

    /** Блокирует строку ссылки до конца транзакции: одну ссылку нельзя использовать дважды параллельно. */
    @Transactional
    public @NotNull Optional<Long> lockUsableLtiRegistrationInvite(@NotNull String tokenHash, @NotNull Instant now) {
        return ltiRegistrationInviteRepository.findLockedByTokenHash(tokenHash)
                .filter(invite -> isUsable(invite, now))
                .map(LtiRegistrationInviteEntity::getId);
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<String> findLtiRegistrationInviteDescription(long inviteId) {
        return ltiRegistrationInviteRepository.findById(inviteId).map(LtiRegistrationInviteEntity::getDescription);
    }

    @Transactional
    public void updateLtiRegistrationDescription(long registrationId, @Nullable String description) {
        if (ltiRegistrationRepository.updateDescription(registrationId, description) != 1) {
            throw new NoSuchElementException("LTI registration " + registrationId + " not found");
        }
    }

    @Transactional
    public void markLtiRegistrationInviteUsed(long inviteId, long registrationId, @NotNull Instant usedAt) {
        var registration = ltiRegistrationRepository.getReferenceById(registrationId);
        if (ltiRegistrationInviteRepository.markUsed(inviteId, registration, usedAt) != 1) {
            throw new IllegalStateException("LTI registration invite " + inviteId + " not found");
        }
    }

    private static boolean isUsable(@NotNull LtiRegistrationInviteEntity invite, @NotNull Instant now) {
        return invite.getUsedAt() == null && invite.getExpiresAt().isAfter(now);
    }
}
