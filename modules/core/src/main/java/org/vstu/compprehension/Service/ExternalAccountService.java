package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.repository.data.ExternalSystemDataRepository;

import java.util.Optional;

/**
 * Связь учётной записи пользователя с его записью во внешней системе.
 * <p>
 * Наружу отдаётся только внешний идентификатор: он — единственное, что из этой связи
 * читают вызывающие, а сущность за границей сервиса означала бы ленивые связи там,
 * где сессии может уже не быть.
 */
@Service
@RequiredArgsConstructor
public class ExternalAccountService {

    private final ExternalSystemDataRepository externalSystems;

    /** Идентификатор пользователя во внешней системе; пусто, если связи нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<String> findExternalId(long userId, long educationResourceId) {
        return externalSystems.findExternalAccountId(userId, educationResourceId);
    }

    /** Завести связь, если её ещё нет. */
    @Transactional
    public void createIfAbsent(long userId, long educationResourceId, @NotNull String externalId) {
        externalSystems.createExternalAccountIfAbsent(userId, educationResourceId, externalId);
    }
}
