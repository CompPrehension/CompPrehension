package org.vstu.compprehension.adapters;

import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;

import java.util.List;
import java.util.Optional;

public class FakeQuestionRequestLogRepository implements QuestionRequestLogRepository {
    @Override
    public List<QuestionRequestLogEntity> findAllNotProcessed(String domainShortName, int countThreshold) {
        return List.of();
    }

    @Override
    public <S extends QuestionRequestLogEntity> S save(S s) {
        return s;
    }

    @Override
    public <S extends QuestionRequestLogEntity> Iterable<S> saveAll(Iterable<S> iterable) {
        return iterable;
    }

    @Override
    public Optional<QuestionRequestLogEntity> findById(Long aLong) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long aLong) {
        return false;
    }

    @Override
    public Iterable<QuestionRequestLogEntity> findAll() {
        return List.of();
    }

    @Override
    public Iterable<QuestionRequestLogEntity> findAllById(Iterable<Long> iterable) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Long aLong) {

    }

    @Override
    public void delete(QuestionRequestLogEntity questionRequestLogEntity) {

    }

    @Override
    public void deleteAll(Iterable<? extends QuestionRequestLogEntity> iterable) {

    }

    @Override
    public void deleteAll() {

    }
}
