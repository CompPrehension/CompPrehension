package org.vstu.compprehension.adapters;

import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.Optional;

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
        e.setShortName("expr");
        e.setVersion("1.0.0");
        e.setOptions(new DomainOptionsEntity());
        return Optional.of(e);
    }

    @Override
    public boolean existsById(String s) {
        return true;
    }

    @Override
    public Iterable<DomainEntity> findAll() {
        return findById(null).stream().toList();
    }

    @Override
    public Iterable<DomainEntity> findAllById(Iterable<String> iterable) {
        return findById(null).stream().toList();
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
    public void deleteAll(Iterable<? extends DomainEntity> iterable) {

    }

    @Override
    public void deleteAll() {

    }
}
