package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FakeDomainRepository implements DomainRepository {
    @Override
    public <S extends DomainEntity> S save(S s) {
        return s;
    }

    @Override
    public <S extends DomainEntity> Iterable<S> saveAll(Iterable<S> iterable) {
        return iterable;
    }

    @Override
    public Optional<DomainEntity> findById(String s) {
        var e = new DomainEntity();
        e.setName("expression");
        e.setShortName("expression");
        e.setVersion("1.0.0");
        e.setOptions(DomainOptionsEntity.builder()
                        .QuestionsGraphPath("C:/Temp2/compp/expression.ttl")
                        .StorageDownloadFilesBaseUrl("file:///C:/Temp2/compp/expression/")
                        .StorageDummyDirsForNewFile(2)
                        .StorageSPARQLEndpointUrl(null)
                        .StorageUploadFilesBaseUrl("file:///C:/Temp2/compp/expression/")
                        .build());
        return Optional.of(e);
    }

    @Override
    public boolean existsById(String s) {
        return true;
    }

    @NotNull
    @Override
    public List<DomainEntity> findAll() {
        return findById("").stream().collect(Collectors.toList());
    }

    @Override
    public Iterable<DomainEntity> findAllById(Iterable<String> iterable) {
        return findById("").stream().collect(Collectors.toList());
    }

    @Override
    public long count() {
        return 1;
    }

    @Override
    public void deleteById(String s) {

    }

    @Override
    public void delete(DomainEntity domainEntity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {

    }

    @Override
    public void deleteAll(Iterable<? extends DomainEntity> iterable) {

    }

    @Override
    public void deleteAll() {

    }
}
