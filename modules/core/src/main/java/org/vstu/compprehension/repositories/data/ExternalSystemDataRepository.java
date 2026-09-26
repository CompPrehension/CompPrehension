package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.data.lti.NewLtiRegistrationData;
import org.vstu.compprehension.data.user.ExternalAccountData;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;
import org.vstu.compprehension.entities.external_system.LtiRegistrationInviteEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.ExternalAccountRepository;
import org.vstu.compprehension.repositories.entity.ExternalAccountRepository.ExternalAccountView;
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
    private final ExternalAccountRepository externalAccountRepository;
    private final LtiRegistrationRepository ltiRegistrationRepository;
    private final LtiRegistrationInviteRepository ltiRegistrationInviteRepository;
    private final UserRepository userRepository;
    private final Mapper<EducationResourceEntity, EducationResourceData> educationResourceMapper;
    private final Mapper<ExternalAccountView, ExternalAccountData> externalAccountMapper;
    private final Mapper<LtiRegistrationEntity, LtiRegistrationData> ltiRegistrationMapper;

    @Transactional(readOnly = true)
    public @NotNull Optional<EducationResourceData> findEducationResource(
            @NotNull String url, @NotNull EducationResourceType type) {
        return educationResourceRepository.findByUrlAndType(url, type)
                .map(educationResourceMapper::map);
    }

    @Transactional(readOnly = true)
    public @NotNull List<EducationResourceData> findEducationResources(
            @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        return educationResourceMapper.mapAll(
                educationResourceRepository.findByTypeAndTrustStatus(type, trustStatus));
    }

    @Transactional(readOnly = true)
    public @NotNull List<ExternalAccountData> findExternalAccounts(long educationResourceId) {
        return externalAccountMapper.mapAll(
                externalAccountRepository.findAccountsByEducationResourceId(educationResourceId));
    }

    @Transactional
    public @NotNull EducationResourceData createEducationResourceIfAbsent(
            @NotNull String url, @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        educationResourceRepository.createIfAbsent(url, type.name(), trustStatus.name());
        return findEducationResource(url, type)
                .orElseThrow(() -> new IllegalStateException(
                        "Education resource " + type + " " + url + " not found after insert"));
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<String> findExternalAccountId(long userId, long educationResourceId) {
        return externalAccountRepository.findExternalId(userId, educationResourceId);
    }

    @Transactional
    public void createExternalAccountIfAbsent(long userId, long educationResourceId,
                                              @NotNull String externalId) {
        externalAccountRepository.createIfAbsent(userId, educationResourceId, externalId);
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
    public @NotNull Optional<LtiRegistrationData> findLtiRegistration(@NotNull String issuer) {
        return ltiRegistrationRepository.findByIssuer(issuer).map(ltiRegistrationMapper::map);
    }

    @Transactional(readOnly = true)
    public @NotNull List<LtiRegistrationData> findAllLtiRegistrations() {
        return ltiRegistrationMapper.mapAll(ltiRegistrationRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public boolean existsLtiRegistration(long educationResourceId) {
        return ltiRegistrationRepository.existsByEducationResourceId(educationResourceId);
    }

    @Transactional
    public @NotNull LtiRegistrationData createLtiRegistration(long educationResourceId,
                                                              @NotNull NewLtiRegistrationData registration) {
        var entity = new LtiRegistrationEntity();
        entity.setEducationResource(educationResourceRepository.getReferenceById(educationResourceId));
        entity.setIssuer(registration.issuer());
        entity.setClientId(registration.clientId());
        entity.setDeploymentId(registration.deploymentId());
        entity.setAuthorizationEndpoint(registration.authorizationEndpoint());
        entity.setTokenEndpoint(registration.tokenEndpoint());
        entity.setJwksUri(registration.jwksUri());
        ltiRegistrationRepository.saveAndFlush(entity);
        return findLtiRegistration(registration.issuer())
                .orElseThrow(() -> new IllegalStateException("LTI registration " + registration.issuer() + " not found after insert"));
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
    public void createLtiRegistrationInvite(@NotNull String tokenHash, long createdByUserId,
                                            @NotNull Instant createdAt, @NotNull Instant expiresAt) {
        var entity = new LtiRegistrationInviteEntity();
        entity.setTokenHash(tokenHash);
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
