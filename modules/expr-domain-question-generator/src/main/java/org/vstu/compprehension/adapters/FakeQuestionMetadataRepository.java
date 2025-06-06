package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.ComplexityStats;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.time.LocalDateTime;
import java.util.Collection;
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
    public List<QuestionMetadataEntity> loadPage(int lastLoadedId, int limit) {
        return List.of();
    }

    @NotNull
    @Override
    public List<QuestionMetadataEntity> loadPageWithData(int lastLoadedId, int limit) {
        return List.of();
    }

    @NotNull
    @Override
    public List<QuestionMetadataEntity> loadPage(int lastLoadedId, String domainShortName, int limit) {
        return List.of();
    }

    @Override
    public long countByDomainShortname(String domainShortname) {
        return 0;
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
    public List<QuestionMetadataEntity> findLastNExerciseAttemptMeta(long attemptId, int limit) {
        return List.of();
    }

    @Override
    public boolean existsByName(String questionName) {
        return false;
    }

    @Override
    public HashSet<String> findExistingNames(String domainShortname, Collection<String> questionNames) {
        return new HashSet<>();
    }

    @Override
    public HashSet<String> findExistingTemplateIds(String domainShortname, Collection<String> templateIds) {
        return new HashSet<>();
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
    public int countQuestions(QuestionBankSearchRequest qr, float complexityWindow) {
        return 0;
    }

    @Override
    public int countTopRatedQuestions(QuestionBankSearchRequest qr, float complexityWindow) {
        return 0;
    }

    @Override
    public List<Integer> findMostUsedMetadataIds(@Nullable Integer weekUsageThreshold, @Nullable Integer dayUsageThreshold, @Nullable Integer hourUsageThreshold, @Nullable Integer min15UsageThreshold, @Nullable Integer min5UsageThreshold) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findTopRatedUnusedMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findTopRatedMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findMetadataRelaxed(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber) {
        return List.of();
    }

    @NotNull
    @Override
    public HashSet<String> findFullyProcessedOrigins(String domainName) {
        return new HashSet<>();
    }

    @NotNull
    @Override
    public HashSet<String> findProcessedOrigins(String domainShortname) {
        return new HashSet<>();
    }

    @NotNull
    @Override
    public HashSet<String> findProcessedOrigins(String domainShortname, LocalDateTime dateFrom) {
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
