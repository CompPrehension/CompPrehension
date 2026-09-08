package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.entities.DomainEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.DomainRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Repository
@RequiredArgsConstructor
public class DomainDataRepository {

    private final DomainRepository domainRepository;
    private final Mapper<DomainEntity, DomainData> domainMapper;

    @Transactional(readOnly = true)
    public @NotNull List<DomainData> findAll() {
        return domainMapper.mapAll(domainRepository.findAll());
    }

    @Transactional(readOnly = true)
    public @NotNull DomainData getById(@NotNull String domainId) {
        return domainMapper.map(domainRepository.findById(domainId)
                .orElseThrow(() -> new NoSuchElementException("Domain " + domainId + " not found")));
    }
}
