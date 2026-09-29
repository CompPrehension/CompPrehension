package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
class EducationResourceServiceImpl implements EducationResourceService {

    private final ExternalSystemDataRepository externalSystems;

    @Transactional(readOnly = true)
    public void ensureTrusted(long educationResourceId) {
        var resource = externalSystems.findEducationResource(educationResourceId)
                .orElseThrow(() -> new NoSuchElementException("Education resource " + educationResourceId + " not found"));
        if (resource.trustStatus() != EducationResourceTrustStatus.TRUSTED) {
            throw new SecurityException(String.format("EducationResource %s is not trusted", resource.url()));
        }
    }
}
