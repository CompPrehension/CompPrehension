package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.entities.QuestionMetadataDraftEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataDraftRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class FakeQuestionMetadataDraftRepository implements QuestionMetadataDraftRepository {
    @NotNull
    @Override
    public List<QuestionMetadataDraftEntity> findByName(String questionName) {
        return List.of();
    }

    @NotNull
    @Override
    public List<String> findAllOrigins(String domainName) {
        return List.of();
    }

    @Override
    public Collection<QuestionMetadataDraftEntity> findSuitableQuestions(QuestionRequestLogEntity qr, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataDraftEntity> findNotYetExportedQuestions(String domainShortname) {
        return List.of();
    }

    @Override
    public <S extends QuestionMetadataDraftEntity> S save(S s) {
        return s;
    }

    @Override
    public <S extends QuestionMetadataDraftEntity> Iterable<S> saveAll(Iterable<S> iterable) {
        return iterable;
    }

    @Override
    public Optional<QuestionMetadataDraftEntity> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Integer integer) {
        return false;
    }

    @Override
    public Iterable<QuestionMetadataDraftEntity> findAll() {
        return List.of();
    }

    @Override
    public Iterable<QuestionMetadataDraftEntity> findAllById(Iterable<Integer> iterable) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(Integer integer) {

    }

    @Override
    public void delete(QuestionMetadataDraftEntity questionMetadataDraftEntity) {

    }

    @Override
    public void deleteAll(Iterable<? extends QuestionMetadataDraftEntity> iterable) {

    }

    @Override
    public void deleteAll() {

    }
}
