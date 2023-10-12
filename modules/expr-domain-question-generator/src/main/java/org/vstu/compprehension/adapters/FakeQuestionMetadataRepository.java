package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.*;

public class FakeQuestionMetadataRepository implements QuestionMetadataRepository {
    @Override
    public <S extends QuestionMetadataEntity> S save(S s) {
        return s;
    }

    @Override
    public <S extends QuestionMetadataEntity> Iterable<S> saveAll(Iterable<S> iterable) {
        return iterable;
    }

    @Override
    public Optional<QuestionMetadataEntity> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Integer integer) {
        return false;
    }

    @NotNull
    @Override
    public Iterable<QuestionMetadataEntity> findAll() {
        return List.of();
    }

    @NotNull
    @Override
    public List<QuestionMetadataEntity> findByName(String questionName) {
        return List.of();
    }

    @Override
    public Iterable<QuestionMetadataEntity> findAllById(Iterable<Integer> iterable) {
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
    public void delete(QuestionMetadataEntity questionMetadataEntity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Integer> integers) {

    }

    @Override
    public void deleteAll(Iterable<? extends QuestionMetadataEntity> iterable) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public Map<String, Object> getStatOnComplexityField(String domainShortName) {
        return new HashMap<>();
    }

    @Override
    public List<QuestionMetadataEntity> findSampleAroundComplexityWithoutQIds(QuestionRequest qr, double complexityWindow, int limitNumber, int randomPoolLimitNumber) {
        return List.of();
    }

    @Override
    public Integer countQuestions(QuestionRequest qr) {
        return 0;
    }

    @NotNull
    @Override
    public List<String> findAllOrigins(String domainName) {
        return List.of();
    }

    @Override
    public Collection<QuestionMetadataEntity> findSuitableQuestions(QuestionRequestLogEntity qr, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findNotYetExportedQuestions(String domainShortname) {
        return List.of();
    }
}
