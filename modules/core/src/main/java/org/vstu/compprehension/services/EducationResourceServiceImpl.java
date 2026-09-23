package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class EducationResourceServiceImpl implements EducationResourceService {

    private final ExternalSystemDataRepository externalSystems;

    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findTrustedIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type) {
        return externalSystems.findEducationResource(url, type)
                .filter(resource -> resource.trustStatus() == EducationResourceTrustStatus.TRUSTED)
                .map(EducationResourceData::id);
    }

    @Transactional
    public @NotNull EducationResourceData getOrCreate(@NotNull String url, @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        return externalSystems.findEducationResource(url, type)
                .orElseGet(() -> externalSystems.createEducationResourceIfAbsent(url, type, trustStatus));
    }
}
