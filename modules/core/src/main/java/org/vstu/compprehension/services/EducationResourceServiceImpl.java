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
    public @NotNull Optional<Long> findIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type) {
        return externalSystems.findEducationResource(url, type).map(EducationResourceData::id);
    }

    @Transactional
    public long getOrCreateTrustedId(@NotNull String url, @NotNull EducationResourceType type) {
        var resource = externalSystems.findEducationResource(url, type)
                .orElseGet(() -> externalSystems.createEducationResourceIfAbsent(url, type));
        if (resource.trustStatus() != EducationResourceTrustStatus.TRUSTED) {
            throw new SecurityException(String.format("EducationResource %s is not trusted", resource.url()));
        }
        return resource.id();
    }
}
