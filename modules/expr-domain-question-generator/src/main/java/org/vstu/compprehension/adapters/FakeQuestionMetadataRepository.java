package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.dto.ComplexityStats;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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
    public boolean existsByName(String questionName) {
        return false;
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
    public ComplexityStats getStatOnComplexityField(String domainShortName) {
        return new ComplexityStats(0L, null, null, null);
    }

    @Override
    public List<QuestionMetadataEntity> findSampleAroundComplexityWithoutQIds(QuestionBankSearchRequest qr, double complexityWindow, int limitNumber, int randomPoolLimitNumber) {
        return List.of();
    }

    @Override
    public int countQuestions(QuestionBankSearchRequest qr) {
        return 0;
    }

    @NotNull
    @Override
    public HashSet<String> findAllOrigins(String domainName) {
        return new HashSet<>();
    }

    @Override
    public boolean templateExists(String domainShortname, String templateId) {
        return false;
    }

    @NotNull
    @Override
    public HashSet<String> findAllTemplates(String domainShortname) {
        return new HashSet<>();
    }
}
