package org.vstu.compprehension.models.repository;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.vstu.compprehension.models.entities.QuestionDataEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class FakeSerializedQuestionRepository implements SerializedQuestionRepository {
    @Override
    public QuestionDataEntity save(QuestionDataEntity questionData) {
        return questionData;
    }

    @Override
    public <S extends QuestionDataEntity> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<QuestionDataEntity> findById(Integer aInteger) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Integer aInteger) {
        return false;
    }

    @Override
    public List<QuestionDataEntity> findAll() {
        return null;
    }

    @Override
    public List<QuestionDataEntity> findAllById(Iterable<Integer> longs) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Integer aInteger) {

    }

    @Override
    public void delete(QuestionDataEntity entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Integer> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends QuestionDataEntity> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public Optional<QuestionDataEntity> findByMetadataId(int questionMetadataId) {
        return Optional.empty();
    }

    @NotNull
    @Override
    public List<QuestionDataEntity> loadPage(int lastLoadedId, int limit) {
        return List.of();
    }

    @Override
    public void flush() {
        
    }

    @Override
    public <S extends QuestionDataEntity> S saveAndFlush(S entity) {
        return entity;
    }

    @Override
    public <S extends QuestionDataEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
        return Lists.newArrayList(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<QuestionDataEntity> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Integer> integers) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public QuestionDataEntity getOne(Integer integer) {
        return null;
    }

    @Override
    public QuestionDataEntity getById(Integer integer) {
        return null;
    }

    @Override
    public QuestionDataEntity getReferenceById(Integer integer) {
        return null;
    }

    @Override
    public <S extends QuestionDataEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends QuestionDataEntity> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends QuestionDataEntity> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends QuestionDataEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends QuestionDataEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends QuestionDataEntity> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends QuestionDataEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<QuestionDataEntity> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<QuestionDataEntity> findAll(Pageable pageable) {
        return null;
    }
}
