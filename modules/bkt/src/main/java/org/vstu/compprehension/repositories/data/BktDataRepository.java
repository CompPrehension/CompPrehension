package org.vstu.compprehension.repositories.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.entities.BktUserDataEntity;
import org.vstu.compprehension.repositories.entity.BktDomainDataRepository;
import org.vstu.compprehension.repositories.entity.BktUserDataRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BktDataRepository {

    private final BktDomainDataRepository bktDomainDataRepository;
    private final BktUserDataRepository bktUserDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public @NotNull Optional<String> findRoster(@NotNull String domainId, long userId) {
        return findEntity(domainId, userId).map(BktUserDataEntity::getRoster);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateRoster(@NotNull String domainId, long userId, @NotNull String roster) {
        findEntity(domainId, userId).ifPresent(entity -> {
            entity.setRoster(roster);
            // Явный flush: конфликт версий должен вылететь здесь, внутри повторяемой
            // операции, а не при коммите — там его уже некому перехватить.
            entityManager.flush();
        });
    }

    private @NotNull Optional<BktUserDataEntity> findEntity(@NotNull String domainId, long userId) {
        var domainData = bktDomainDataRepository.findById(domainId).orElse(null);
        if (domainData == null) {
            return Optional.empty();
        }
        // Пустой roster области — шаблон для новых студентов; без него BKT не работает.
        var emptyRoster = domainData.getEmptyRoster();
        if (emptyRoster == null || emptyRoster.isBlank()) {
            return Optional.empty();
        }

        var dataId = new BktUserDataEntity.BktUserDataId(userId, domainId);
        return Optional.of(bktUserDataRepository.findById(dataId).orElseGet(() -> {
            var newData = new BktUserDataEntity();
            newData.setUserId(userId);
            newData.setDomainName(domainId);
            newData.setRoster(emptyRoster);
            try {
                return bktUserDataRepository.save(newData);
            } catch (DataIntegrityViolationException ignored) {
                // Строку успел завести параллельный запрос того же студента.
                return bktUserDataRepository.findById(dataId).orElseThrow();
            }
        }));
    }
}
