package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ExternalAccountService {
    @NotNull Optional<String> findExternalId(long userId, long educationResourceId);

    void createIfAbsent(long userId, long educationResourceId, @NotNull String externalId);
}
