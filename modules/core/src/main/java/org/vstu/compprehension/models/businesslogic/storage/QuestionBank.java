package org.vstu.compprehension.models.businesslogic.storage;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.date.DateTimeProvider;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.repository.QuestionDataRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataSearchRequestRepository;
import org.vstu.compprehension.utils.transactions.TransactionScope;
import org.vstu.compprehension.utils.transactions.TransactionScopeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class QuestionBank {
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionDataRepository questionDataRepository;
    private final QuestionMetadataManager questionMetadataManager;
    private final QuestionGenerationRequestRepository generationRequestRepository;
    private final QuestionMetadataSearchRequestRepository questionSearchRequestLogRepository;
    private final TransactionScope logSavingTransactionScope;
    private final DateTimeProvider dateTimeProvider;

    public QuestionBank(
            QuestionMetadataRepository questionMetadataRepository,
            QuestionDataRepository questionDataRepository,
            QuestionGenerationRequestRepository generationRequestRepository,
            QuestionMetadataSearchRequestRepository questionSearchRequestLogRepository,
            TransactionScopeFactory transactionScopeFactory, DateTimeProvider dateTimeProvider) {
        this.questionMetadataRepository = questionMetadataRepository;
        this.questionDataRepository = questionDataRepository;
        this.questionMetadataManager = new QuestionMetadataManager(questionMetadataRepository, dateTimeProvider);
        this.generationRequestRepository = generationRequestRepository;
        this.questionSearchRequestLogRepository = questionSearchRequestLogRepository;
        this.dateTimeProvider = dateTimeProvider;
        
        // for actions that must be executed in new transaction (like save logs)
        this.logSavingTransactionScope = transactionScopeFactory.create(TransactionScope.PropagationBehavior.REQUIRES_NEW);
    }

    public QuestionBankSearchRequest createBankSearchRequest(QuestionRequest qr) {
        var minComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMax();

        return QuestionBankSearchRequest.fromQuestionRequest(qr, minComplexity, maxComplexity);
    }

    public boolean isMatch(@NotNull QuestionMetadataEntity meta, @NotNull QuestionRequestLogEntity qrLog) {
        var minComplexity = questionMetadataManager.getComplexityStats(qrLog.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qrLog.getDomainShortname()).getMax();

        var bankSearchRequest = QuestionBankSearchRequest.fromQuestionRequestLog(qrLog, minComplexity, maxComplexity);
        return isMatch(meta, bankSearchRequest);
    }

    /**
     * Реализует логику, равносильную QuestionMetadataComplexQueriesRepository.findTopRatedMetadata, для сопоставления сгенерированного вопроса с запросом на поиск в банке.
     * @param meta метаданные сгенерированного вопроса
     * @param qr поисковый запрос к банку вопросов (complexity нормализована на диапазон сложности в банке)
     * @return true, если имеет место совпадение вопроса с поисковым запросом
     */
    public boolean isMatch(@NotNull QuestionMetadataEntity meta, @NotNull QuestionBankSearchRequest qr) {
        // Если не совпадает имя домена – мы пытаемся сделать что-то Неправильно!
        if (qr.getDomainShortname() != null && ! qr.getDomainShortname().equalsIgnoreCase(meta.getDomainShortname())) {
            throw new RuntimeException(String.format("Trying matching a question with a QuestionRequest(LogEntity) of different domain ! (%s != %s)", meta.getDomainShortname(), qr.getDomainShortname()));
        }

        // проверка запрещаемых критериев
        if (meta.getSolutionSteps() < qr.getStepsMin()
                || qr.getStepsMax() != 0 && meta.getSolutionSteps() > qr.getStepsMax()
                || qr.getDeniedConceptsBitmask() != 0 && (meta.getConceptBits() & qr.getDeniedConceptsBitmask()) != 0
                || qr.getDeniedLawsBitmask() != 0 && (meta.getViolationBits() & qr.getDeniedLawsBitmask()) != 0
                || qr.getDeniedSkillsBitmask() != 0 && (meta.getSkillBits() & qr.getDeniedSkillsBitmask()) != 0
                || qr.getTargetTagsBitmask() != 0 && (meta.getTagBits() & qr.getTargetTagsBitmask()) != qr.getTargetTagsBitmask() // требуем наличия всех тэгов из qr
        ) {
            return false;
        }

        // Если есть запрет по ID шаблона/метаданных или имени вопроса
        if (qr.getDeniedQuestionTemplateIds() != null && !qr.getDeniedQuestionTemplateIds().isEmpty() && qr.getDeniedQuestionTemplateIds().contains(meta.getTemplateId())
                || qr.getDeniedQuestionNames() != null && !qr.getDeniedQuestionNames().isEmpty() && qr.getDeniedQuestionNames().contains(meta.getName())
                || qr.getDeniedQuestionMetaIds() != null && meta.getId() != null && !qr.getDeniedQuestionMetaIds().isEmpty() && qr.getDeniedQuestionMetaIds().contains(meta.getId())) {
            return false;
        }

        // сложность должна быть в пределах COMPLEXITY_WINDOW от запрашиваемой
        if (qr.getComplexity() != 0 && Math.abs(qr.getComplexity() - meta.getIntegralComplexity()) > qr.getComplexityWindow()) {
            return false;
        }

        // Присутствует хотя бы один из целевых концептов и законов
        if (qr.getTargetConceptsBitmask() != 0 && (meta.getConceptBits() & qr.getTargetConceptsBitmask()) == 0
                || qr.getTargetLawsBitmask() != 0 && (meta.getViolationBits() & qr.getTargetLawsBitmask()) == 0
                // Note: ↑ violation в meta — это негативные законы (нарушения), в текущей редакции сопоставляются с negative laws, которые настраиваются в упражнении.
        ) {
            return false;
        }

        // Присутствует хотя бы один из целевых скиллов
        if (qr.getTargetSkillsBitmask() != 0 && (meta.getSkillBits() & qr.getTargetSkillsBitmask()) == 0) {
            return false;
        }

        /*
        // Присутствует хотя бы половина целевых концептов и законов
        if (qr.getTargetConceptsBitmask() != 0 && ((meta.getConceptBits() & qr.getTargetConceptsBitmask()) == 0 || Long.bitCount(meta.getConceptBits() & qr.getTargetConceptsBitmask()) < Long.bitCount(qr.getTargetConceptsBitmask()) / 2)
                || qr.getTargetLawsBitmask() != 0 && ((meta.getViolationBits() & qr.getTargetLawsBitmask()) == 0 || Long.bitCount(meta.getLawBits() & qr.getTargetLawsBitmask()) < Long.bitCount(qr.getTargetLawsBitmask()) / 2)
        ) {
            return false;
        }
        */

        return true;
    }

    public int countQuestions(QuestionRequest qr) {
        var bankSearchRequest = createBankSearchRequest(qr);
        return questionMetadataRepository.countQuestions(bankSearchRequest);
    }

    public QuestionBankSearchStatsDto getStatsByQuestionRequest(QuestionRequest qr, int limit) {
        var bankSearchRequest = createBankSearchRequest(qr);
        var ordinaryCount = questionMetadataRepository.countQuestions(bankSearchRequest);
        var topRatedCount = questionMetadataRepository.countTopRatedQuestions(bankSearchRequest);
        var metadata = questionMetadataRepository.findMetadata(bankSearchRequest, limit)
            .stream()
            .map(m -> new QuestionBankSearchStatsDto.QuestionMetadataDto(m.getId(), m.getName()))
            .toList();
        return new QuestionBankSearchStatsDto(ordinaryCount, topRatedCount, metadata);
    }

    public QuestionBankSearchResult searchQuestions(@NotNull QuestionRequest qr, int limit, int generatorThreshold, int generatorAdditionalQuestionsToGenerate) {

        var bankSearchRequest = createBankSearchRequest(qr);
        
        var prevQuestionsMetadata = qr.getExerciseAttemptId() != null
            ? questionMetadataRepository.findLastNExerciseAttemptMeta(qr.getExerciseAttemptId(), 4)
            : List.<QuestionMetadataEntity>of();

        long targetConceptsBitmaskInPlan = bankSearchRequest.getTargetConceptsBitmask();
        long targetConceptsBitmask = targetConceptsBitmaskInPlan;
        long deniedConceptsBitmask = bankSearchRequest.getDeniedConceptsBitmask();
        long unwantedConceptsBitmask = prevQuestionsMetadata.stream()
                .mapToLong(QuestionMetadataEntity::getConceptBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetConceptsBitmask &= ~deniedConceptsBitmask;


        // use laws, for e.g. Expr domain
        long targetViolationsBitmaskInPlan = bankSearchRequest.getTargetLawsBitmask();
        long targetLawsBitmask = targetViolationsBitmaskInPlan;
        long deniedLawsBitmask = bankSearchRequest.getDeniedLawsBitmask();
        long unwantedLawsBitmask = prevQuestionsMetadata.stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetLawsBitmask &= ~deniedLawsBitmask;

        // use violations from all questions is exercise attempt
        long unwantedViolationsBitmask = prevQuestionsMetadata.stream()
                .mapToLong(QuestionMetadataEntity::getViolationBits)
                .reduce((t, t2) -> t | t2).orElse(0);

        long targetSkillsBitmaskInPlan = bankSearchRequest.getTargetSkillsBitmask();
        long targetSkillsBitmask = targetSkillsBitmaskInPlan;
        long deniedSkillsBitmask = bankSearchRequest.getDeniedSkillsBitmask();
        long unwantedSkillsBitmask = prevQuestionsMetadata.stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetSkillsBitmask &= ~deniedSkillsBitmask;

        // ensure generatorThreshold & generatorAdditionalQuestionsToGenerate is valid
        if (generatorThreshold < 0) {
            generatorThreshold = 0;
        }
        if (generatorAdditionalQuestionsToGenerate < 0) {
            generatorAdditionalQuestionsToGenerate = 0;
        }

        var searchSteps = new ArrayList<QuestionMetadataSearchRequestEntity.Iteration>(3);
        List<QuestionMetadataEntity> foundQuestionMetas;

        var preparedQuery = bankSearchRequest.toBuilder()
                .targetConceptsBitmask(targetConceptsBitmask)
                .targetLawsBitmask(targetLawsBitmask)
                .targetSkillsBitmask(targetSkillsBitmask)
                .unwantedConceptsBitmask(unwantedConceptsBitmask)
                .unwantedLawsBitmask(unwantedLawsBitmask)
                .unwantedSkillsBitmask(unwantedSkillsBitmask)
                .unwantedViolationsBitmask(unwantedViolationsBitmask)
                .generatorThreshold(generatorThreshold)
                .generatorAdditionalQuestionsToGenerate(generatorAdditionalQuestionsToGenerate)
                .build();
        log.debug("problem search query prepared: {}", new Gson().toJson(preparedQuery));

        {
            int topRatedLimit = generatorThreshold + 3;
            log.debug("trying to do {} search with {} limit", QuestionMetadataSearchRequestEntity.Quality.BestUnused, topRatedLimit);
            foundQuestionMetas = questionMetadataRepository.findTopRatedUnusedMetadata(preparedQuery, topRatedLimit);
            log.info("search executed with {} strategy and returns {} problems found ({} requested, {} generatorThreshold)", QuestionMetadataSearchRequestEntity.Quality.BestUnused, foundQuestionMetas.size(), topRatedLimit, generatorThreshold);
            searchSteps.add(new QuestionMetadataSearchRequestEntity.Iteration(QuestionMetadataSearchRequestEntity.Quality.BestUnused, topRatedLimit, foundQuestionMetas.size()));
        }

        // runtime assert to find possible desync between findTopRatedMetadata and isMatch methods
        {
            List<Integer> notMatchedMetadata = null;
            for (QuestionMetadataEntity question : foundQuestionMetas) {
                if (!isMatch(question, preparedQuery)) {
                    if (notMatchedMetadata == null)
                        notMatchedMetadata = new ArrayList<>();
                    notMatchedMetadata.add(question.getId());
                }
            }
            if (notMatchedMetadata != null) {
                log.error("isMatch desync detected. Metadata with ids={} does not match bank search query {}", notMatchedMetadata, new Gson().toJson(preparedQuery));
            }
        }

        if (foundQuestionMetas.size() <= generatorThreshold) {
            log.info("too few top rated problems found ({}/{}), need additional generation", foundQuestionMetas.size(), generatorThreshold);

            // calculate how many questions to generate based on the number of found questions and existing generation requests
            var rawQuestionsToGenerate = generatorThreshold + generatorAdditionalQuestionsToGenerate - foundQuestionMetas.size(); // +generatorAdditionalQuestionsToGenerate additional questions to be sure that we will have enough
            var currentlyGeneratingQuestions = generationRequestRepository.findNumberOfCurrentlyGeneratingQuestions(qr.getDomainShortname(), preparedQuery);
            var questionsToGenerate = Math.max(1, rawQuestionsToGenerate - currentlyGeneratingQuestions);
            var genRequest = logSavingTransactionScope.execute(() -> {
                var generationRequest = new QuestionGenerationRequestEntity(preparedQuery, questionsToGenerate, qr.getExerciseAttemptId());
                return generationRequestRepository.save(generationRequest);
            });
            log.info("created generation request with id {} with {} problems to generate", genRequest.getId(), genRequest.getQuestionsToGenerate());
        }
        
        if (foundQuestionMetas.isEmpty()) {
            int normalLimit = 100;
            log.debug("trying to do {} search with {} limit", QuestionMetadataSearchRequestEntity.Quality.Normal, normalLimit);
            foundQuestionMetas = questionMetadataRepository.findMetadata(preparedQuery, normalLimit);
            log.info("search executed with {} strategy and returns {} problems ({} requested)", QuestionMetadataSearchRequestEntity.Quality.Normal, foundQuestionMetas.size(), normalLimit);
            searchSteps.add(new QuestionMetadataSearchRequestEntity.Iteration(QuestionMetadataSearchRequestEntity.Quality.Normal, normalLimit, foundQuestionMetas.size()));
        }
        
        if (foundQuestionMetas.isEmpty()) {
            int relaxedLimit = 100;
            log.debug("trying to do {} search with {} limit", QuestionMetadataSearchRequestEntity.Quality.Relaxed, relaxedLimit);
            foundQuestionMetas = questionMetadataRepository.findMetadataRelaxed(preparedQuery, relaxedLimit);
            log.info("search executed with {} strategy and returns {} problems", QuestionMetadataSearchRequestEntity.Quality.Relaxed, foundQuestionMetas.size());
            searchSteps.add(new QuestionMetadataSearchRequestEntity.Iteration(QuestionMetadataSearchRequestEntity.Quality.Relaxed, relaxedLimit, foundQuestionMetas.size()));
        }

        foundQuestionMetas = foundQuestionMetas.subList(0, Math.min(limit, foundQuestionMetas.size()));
        
        // set concepts from request (for future reference via questions' saved metadata)
        for (QuestionMetadataEntity m : foundQuestionMetas) {
            m.setConceptBitsInPlan(targetConceptsBitmaskInPlan);
            m.setViolationBitsInPlan(targetViolationsBitmaskInPlan);
            m.setSkillBitsInPlan(targetSkillsBitmaskInPlan);
            // Save actual requested bits as well
            m.setConceptBitsInRequest(targetConceptsBitmask);
            m.setViolationBitsInRequest(targetLawsBitmask);

            /// debug check law bits
            if (targetLawsBitmask != 0 && (targetLawsBitmask & m.getViolationBits()) == 0) {
                log.warn("No LAW bits matched: {} {}", targetLawsBitmask, m.getName());
            }
        }

        // save search request to db
        var logEntity = logSavingTransactionScope.execute(() -> {
            var entity = new QuestionMetadataSearchRequestEntity(preparedQuery, searchSteps, qr.getId());
            return questionSearchRequestLogRepository.save(entity);
        });

        return new QuestionBankSearchResult(logEntity.getQuality(), foundQuestionMetas);
    }

    public @Nullable QuestionMetadataEntity loadQuestion(int questionMetadataId) {
        try {
            var questionMeta = questionMetadataRepository.findById(questionMetadataId)
                    .orElse(null);
            if (questionMeta != null) {
                return questionMeta;
            }
            log.warn("Question data NOT found for metadata id: {}", questionMetadataId);
        } catch (Exception e) {
            log.error("Error loading question with metadata id [{}] - {}", questionMetadataId, e.getMessage(), e);
        }
        return null;
    }

    public boolean questionExists(String questionName) {
        val repo = this.questionMetadataRepository;
        if (repo == null)
            return false;

        return repo.existsByName(questionName);
    }

    @NotNull
    public QuestionMetadataEntity saveMetadataEntity(QuestionMetadataEntity meta) {
        return questionMetadataRepository.save(meta);
    }

    public void saveMetadataWithDataEntities(List<QuestionMetadataEntity> metas) {
        var allData = metas.stream()
                .map(QuestionMetadataEntity::getQuestionData)
                .collect(Collectors.toSet());
        questionDataRepository.saveAll(allData);
        questionMetadataRepository.saveAll(metas);
    }

    public QuestionDataEntity saveQuestionDataEntity(QuestionDataEntity questionData) {
        return questionDataRepository.save(questionData);
    }
}
