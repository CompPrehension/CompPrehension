package org.vstu.compprehension.adapters;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

public class FakeQuestionMetadataRepository implements QuestionMetadataRepository {
    @Override
    public <S extends QuestionMetadataEntity> S save(S s) {
        return s;
    }

    @Override
    public <S extends QuestionMetadataEntity> List<S> saveAll(Iterable<S> iterable) {
        return Lists.newArrayList(iterable.iterator());
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
    public List<QuestionMetadataEntity> findAll() {
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
    public List<QuestionMetadataEntity> findAllById(Iterable<Integer> iterable) {
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
    public void flush() {
        
    }

    @Override
    public <S extends QuestionMetadataEntity> S saveAndFlush(S entity) {
        return entity;
    }

    @Override
    public <S extends QuestionMetadataEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
        return List.of();
    }

    @Override
    public void deleteAllInBatch(Iterable<QuestionMetadataEntity> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Integer> integers) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public QuestionMetadataEntity getOne(Integer integer) {
        return null;
    }

    @Override
    public QuestionMetadataEntity getById(Integer integer) {
        return null;
    }

    @Override
    public QuestionMetadataEntity getReferenceById(Integer integer) {
        return null;
    }

    @Override
    public <S extends QuestionMetadataEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends QuestionMetadataEntity> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends QuestionMetadataEntity> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends QuestionMetadataEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends QuestionMetadataEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends QuestionMetadataEntity> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends QuestionMetadataEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<QuestionMetadataEntity> findAll(Sort sort) {
        return List.of();
    }

    @Override
    public Page<QuestionMetadataEntity> findAll(Pageable pageable) {
        return null;
    }

    /** Заглушка: у генератора нет статистики по банку. */
    private record EmptyComplexityStats() implements QuestionMetadataRepository.ComplexityStatsView {
        @Override public Long getCount() { return 0L; }
        @Override public Double getMin() { return null; }
        @Override public Double getMean() { return null; }
        @Override public Double getMax() { return null; }
    }

    @Override
    public QuestionMetadataRepository.ComplexityStatsView getStatOnComplexityField(String domainShortName) {
        return new EmptyComplexityStats();
    }

    @Override
    public int countQuestions(QuestionBankSearchRequest qrw) {
        return 0;
    }

    @Override
    public int countTopRatedQuestions(QuestionBankSearchRequest qr) {
        return 0;
    }

    @Override
    public List<Integer> findMostUsedMetadataIds(@Nullable Integer weekUsageThreshold, @Nullable Integer dayUsageThreshold, @Nullable Integer hourUsageThreshold, @Nullable Integer min15UsageThreshold, @Nullable Integer min5UsageThreshold) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findTopRatedUnusedMetadata(QuestionBankSearchRequest qr, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findTopRatedMetadata(QuestionBankSearchRequest qr, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findMetadata(QuestionBankSearchRequest qr, int limitNumber) {
        return List.of();
    }

    @Override
    public List<QuestionMetadataEntity> findMetadataRelaxed(QuestionBankSearchRequest qr, int limitNumber) {
        return List.of();
    }

    @Override
    public int deleteMetadataFromDate(LocalDate date) {
        return 0;
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
