package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.data.question.QuestionMaskData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.data.questionbank.ComplexityStatsData;
import org.vstu.compprehension.data.questionbank.GenerationRequestGroupData;
import org.vstu.compprehension.data.questionbank.NewBankQuestionData;
import org.vstu.compprehension.data.questionbank.SearchIterationData;
import org.vstu.compprehension.data.questionbank.SearchQuality;
import org.vstu.compprehension.entities.QuestionDataEntity;
import org.vstu.compprehension.entities.QuestionGenerationRequestEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.entities.QuestionMetadataSearchRequestEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.QuestionGenerationRequestRepository;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository.QuestionMaskView;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository;
import org.vstu.compprehension.repositories.entity.QuestionMetadataSearchRequestRepository;
import org.vstu.compprehension.repositories.entity.SerializedQuestionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class QuestionBankDataRepository {

    private final QuestionMetadataRepository metadataRepository;
    private final SerializedQuestionRepository serializedQuestionRepository;
    private final QuestionGenerationRequestRepository generationRequestRepository;
    private final QuestionMetadataSearchRequestRepository searchRequestLogRepository;
    private final Mapper<QuestionMaskView, QuestionMaskData> questionMaskMapper;
    private final Mapper<QuestionMetadataEntity, QuestionMetadataData> questionMetadataMapper;
    private final Mapper<QuestionMetadataEntity, QuestionMetadataWithData> questionMetadataWithDataMapper;
    private final Mapper<QuestionMetadataData, QuestionMetadataEntity> questionMetadataEntityMapper;


    @Transactional(readOnly = true)
    public @NotNull ComplexityStatsData getComplexityStats(@NotNull String domainShortname) {
        var stats = metadataRepository.getStatOnComplexityField(domainShortname);
        return new ComplexityStatsData(
                stats.getCount() == null ? 0L : stats.getCount(),
                stats.getMin(), stats.getMean(), stats.getMax());
    }

    @Transactional(readOnly = true)
    public int countQuestions(@NotNull QuestionBankSearchRequest request) {
        return metadataRepository.countQuestions(request);
    }

    @Transactional(readOnly = true)
    public int countTopRatedQuestions(@NotNull QuestionBankSearchRequest request) {
        return metadataRepository.countTopRatedQuestions(request);
    }

    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataWithData> findTopRatedUnusedMetadata(
            @NotNull QuestionBankSearchRequest request, int limit) {
        var found = metadataRepository.findTopRatedUnusedMetadata(request, limit);
        fetchBodies(found);
        return questionMetadataWithDataMapper.mapAll(found);
    }

    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataWithData> findMetadata(
            @NotNull QuestionBankSearchRequest request, int limit) {
        var found = metadataRepository.findMetadata(request, limit);
        fetchBodies(found);
        return questionMetadataWithDataMapper.mapAll(found);
    }

    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataWithData> findMetadataRelaxed(
            @NotNull QuestionBankSearchRequest request, int limit) {
        var found = metadataRepository.findMetadataRelaxed(request, limit);
        fetchBodies(found);
        return questionMetadataWithDataMapper.mapAll(found);
    }

    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataData> findMetadataWithoutBodies(
            @NotNull QuestionBankSearchRequest request, int limit) {
        return questionMetadataMapper.mapAll(metadataRepository.findMetadata(request, limit));
    }

    @Transactional(readOnly = true)
    public @NotNull List<QuestionMaskData> findRecentAttemptQuestionMasks(long attemptId, int limit) {
        return questionMaskMapper.mapAll(
                metadataRepository.findRecentAttemptQuestionMasks(attemptId, limit));
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<QuestionMetadataWithData> findMetadataById(int metadataId) {
        return metadataRepository.findByIdFetchingData(metadataId)
                .filter(e -> e.getQuestionData() != null && e.getQuestionData().getData() != null)
                .map(questionMetadataWithDataMapper::map);
    }

    @Transactional(readOnly = true)
    public boolean questionExists(@NotNull String questionName) {
        return metadataRepository.existsByName(questionName);
    }

    @Transactional(readOnly = true)
    public long countByDomain(@NotNull String domainShortname) {
        return metadataRepository.countByDomainShortname(domainShortname);
    }

    @Transactional(readOnly = true)
    public @NotNull Set<String> findExistingNames(@NotNull String domainShortname,
                                                  @NotNull Collection<String> questionNames) {
        return questionNames.isEmpty() ? Set.of()
                : metadataRepository.findExistingNames(domainShortname, questionNames);
    }

    @Transactional(readOnly = true)
    public @NotNull Set<String> findExistingTemplateIds(@NotNull String domainShortname,
                                                        @NotNull Collection<String> templateIds) {
        return templateIds.isEmpty() ? Set.of()
                : metadataRepository.findExistingTemplateIds(domainShortname, templateIds);
    }

    @Transactional(readOnly = true)
    public @NotNull Set<String> findProcessedOrigins(@NotNull String domainShortname,
                                                     @NotNull LocalDateTime since) {
        return metadataRepository.findProcessedOrigins(domainShortname, since);
    }

    @Transactional
    public int saveQuestions(@NotNull List<NewBankQuestionData> questions) {
        var bodies = new ArrayList<QuestionDataEntity>(questions.size());
        var metadata = new ArrayList<QuestionMetadataEntity>();
        for (NewBankQuestionData question : questions) {
            var body = new QuestionDataEntity();
            body.setData(question.body());
            bodies.add(body);
            for (QuestionMetadataData meta : question.metadata()) {
                var entity = questionMetadataEntityMapper.map(meta);
                entity.setQuestionData(body);
                metadata.add(entity);
            }
        }
        serializedQuestionRepository.saveAll(bodies);
        metadataRepository.saveAll(metadata);
        return metadata.size();
    }

    @Transactional
    public void replaceQuestionBody(int metadataId,
                                    @NotNull SerializableQuestion body) {
        var metadata = metadataRepository.findById(metadataId).orElseThrow(
                () -> new NoSuchElementException("Question metadata " + metadataId + " not found"));
        var entity = new QuestionDataEntity();
        entity.setData(body);
        metadata.setQuestionData(serializedQuestionRepository.save(entity));
        metadataRepository.save(metadata);
    }


    @Transactional
    public int deleteMetadataFromDate(@NotNull LocalDate date) {
        return metadataRepository.deleteMetadataFromDate(date);
    }


    @Transactional(readOnly = true)
    public int countCurrentlyGeneratingQuestions(@NotNull String domainShortname,
                                                 @NotNull QuestionBankSearchRequest request) {
        return generationRequestRepository.findNumberOfCurrentlyGeneratingQuestions(domainShortname, request);
    }

    @Transactional(readOnly = true)
    public @NotNull List<GenerationRequestGroupData> findActualGenerationRequests(
            @NotNull String domainShortname, @NotNull LocalDateTime createdAfter) {
        return generationRequestRepository.findAllActual(domainShortname, createdAfter);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int createGenerationRequest(@NotNull QuestionBankSearchRequest request,
                                       int questionsToGenerate, @Nullable Long exerciseAttemptId) {
        var entity = new QuestionGenerationRequestEntity(request, questionsToGenerate, exerciseAttemptId);
        return generationRequestRepository.save(entity).getId();
    }

    @Transactional
    public void refreshGenerationRequests(@NotNull Integer[] generationRequestIds) {
        generationRequestRepository.updateGenerationRequests(generationRequestIds);
    }

    @Transactional(readOnly = true)
    public @NotNull List<Integer> findStuckGenerationRequestIds(int processingAttemptsGreaterThan) {
        return generationRequestRepository.findAllIdsByStatusAndProcessingAttemptsGreaterThan(
                QuestionGenerationRequestEntity.Status.ACTUAL, processingAttemptsGreaterThan);
    }

    @Transactional
    public void cancelGenerationRequests(@NotNull List<Integer> generationRequestIds) {
        generationRequestRepository.setCancelled(generationRequestIds);
    }

    @Transactional
    public int cancelAllActiveGenerationRequests() {
        var cancelled = generationRequestRepository.cancelAllActiveRequests();
        return cancelled == null ? 0 : cancelled;
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findLastGenerationRequestIdOfAttempt(long exerciseAttemptId) {
        return generationRequestRepository.getLastRequestByExerciseAttemptId(exerciseAttemptId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public @NotNull SearchQuality logSearch(@NotNull QuestionBankSearchRequest request,
                                            @NotNull List<SearchIterationData> iterations,
                                            @Nullable UUID questionRequestId) {
        var entity = new QuestionMetadataSearchRequestEntity(request, iterations, questionRequestId);
        return searchRequestLogRepository.save(entity).getQuality();
    }

    /** Тела вопросов лежат в отдельной таблице и подтягиваются одним запросом на всю пачку. */
    private void fetchBodies(@NotNull List<QuestionMetadataEntity> metadata) {
        if (!metadata.isEmpty()) {
            metadataRepository.fetchQuestionData(
                    metadata.stream().map(QuestionMetadataEntity::getId).toList());
        }
    }
}
