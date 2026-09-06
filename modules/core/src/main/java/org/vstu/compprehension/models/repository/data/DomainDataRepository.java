package org.vstu.compprehension.models.repository.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.DomainData;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Справочник предметных областей.
 * <p>
 * Читается один раз при старте — фабрика доменов собирает по этим описаниям объекты
 * {@code Domain} и дальше живёт без базы. Раньше каждая из двух фабрик (серверная и
 * фоновая) сама превращала сущность в {@link DomainData}, и обе одинаково полагались на
 * то, что json-колонка с настройками не пуста.
 */
@Repository
@RequiredArgsConstructor
public class DomainDataRepository {

    private final DomainRepository domainRepository;

    /** Все заведённые предметные области. */
    @Transactional(readOnly = true)
    public @NotNull List<DomainData> findAll() {
        return domainRepository.findAll().stream().map(DomainDataRepository::toData).toList();
    }

    /**
     * Предметная область по идентификатору, он же её имя.
     *
     * @throws NoSuchElementException если области нет
     */
    @Transactional(readOnly = true)
    public @NotNull DomainData getById(@NotNull String domainId) {
        return toData(domainRepository.findById(domainId)
                .orElseThrow(() -> new NoSuchElementException("Domain " + domainId + " not found")));
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull DomainData toData(@NotNull DomainEntity entity) {
        String name = Strict.required(entity.getName(), "name", "domain");
        return new DomainData(
                name,
                Strict.required(entity.getShortName(), "shortName", "domain " + name),
                Strict.required(entity.getVersion(), "version", "domain " + name),
                // Настройки объявлены not null в схеме, но это значение json-колонки:
                // пустой текст в ней даёт null уже после успешного чтения строки.
                Strict.required(entity.getOptions(), "options", "domain " + name));
    }
}
