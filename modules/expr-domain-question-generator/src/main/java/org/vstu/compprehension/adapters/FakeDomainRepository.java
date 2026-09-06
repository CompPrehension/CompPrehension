package org.vstu.compprehension.adapters;

import com.google.common.collect.Lists;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.vstu.compprehension.models.data.DomainOptionsData;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.repository.DomainRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FakeDomainRepository implements DomainRepository {
    @Override
    public <S extends DomainEntity> S save(S s) {
        return s;
    }

    @Override
    public <S extends DomainEntity> List<S> saveAll(Iterable<S> iterable) {
        return Lists.newArrayList(iterable);
    }

    @Override
    public Optional<DomainEntity> findById(String s) {
        var e = new DomainEntity();
        e.setName("expression");
        e.setShortName("expression");
        e.setVersion("1.0.0");
        e.setOptions(new DomainOptionsData());
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
    public List<DomainEntity> findAllById(Iterable<String> iterable) {
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

    @Override
    public void flush() {
        
    }

    @Override
    public <S extends DomainEntity> S saveAndFlush(S entity) {
        return entity;
    }

    @Override
    public <S extends DomainEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
        return Lists.newArrayList(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<DomainEntity> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<String> strings) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public DomainEntity getOne(String s) {
        return null;
    }

    @Override
    public DomainEntity getById(String s) {
        return null;
    }

    @Override
    public DomainEntity getReferenceById(String s) {
        return null;
    }

    @Override
    public <S extends DomainEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends DomainEntity> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends DomainEntity> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends DomainEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends DomainEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends DomainEntity> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends DomainEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<DomainEntity> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<DomainEntity> findAll(Pageable pageable) {
        return null;
    }
}
