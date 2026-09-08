package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.data.user.ExternalAccountData;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.EducationResourceRepository;
import org.vstu.compprehension.repositories.entity.ExternalAccountRepository;
import org.vstu.compprehension.repositories.entity.ExternalAccountRepository.ExternalAccountView;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExternalSystemDataRepository {

    private final EducationResourceRepository educationResourceRepository;
    private final ExternalAccountRepository externalAccountRepository;
    private final Mapper<EducationResourceEntity, EducationResourceData> educationResourceMapper;
    private final Mapper<ExternalAccountView, ExternalAccountData> externalAccountMapper;

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
            @NotNull String url, @NotNull EducationResourceType type) {
        educationResourceRepository.createIfAbsent(url, type.name());
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
}
