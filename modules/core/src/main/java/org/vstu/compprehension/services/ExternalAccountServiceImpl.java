package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.repositories.data.ExternalSystemDataRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class ExternalAccountServiceImpl implements ExternalAccountService {

    private final ExternalSystemDataRepository externalSystems;

    @Transactional(readOnly = true)
    public @NotNull Optional<String> findExternalId(long userId, long educationResourceId) {
        return externalSystems.findExternalAccountId(userId, educationResourceId);
    }

    @Transactional
    public void createIfAbsent(long userId, long educationResourceId, @NotNull String externalId) {
        externalSystems.createExternalAccountIfAbsent(userId, educationResourceId, externalId);
    }
}
