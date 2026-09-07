package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.entities.DomainEntity;
import org.vstu.compprehension.repositories.entity.DomainRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Repository
@RequiredArgsConstructor
public class DomainDataRepository {

    private final DomainRepository domainRepository;

    @Transactional(readOnly = true)
    public @NotNull List<DomainData> findAll() {
        return domainRepository.findAll().stream().map(DomainDataRepository::toData).toList();
    }

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
                Strict.required(entity.getOptions(), "options", "domain " + name));
    }
}
