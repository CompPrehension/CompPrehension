package org.vstu.compprehension.adapters;

import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.repository.QuestionDataRepository;

import java.util.Optional;

public class FakeQuestionDataRepository implements QuestionDataRepository {
    @Override
    public QuestionDataEntity save(QuestionDataEntity questionData) {
        return questionData;
    }

    @Override
    public <S extends QuestionDataEntity> Iterable<S> saveAll(Iterable<S> entities) {
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
    public Iterable<QuestionDataEntity> findAll() {
        return null;
    }

    @Override
    public Iterable<QuestionDataEntity> findAllById(Iterable<Integer> longs) {
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
}
