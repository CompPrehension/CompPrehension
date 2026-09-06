package org.vstu.compprehension.models.businesslogic.storage;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.data.GenerationRequestGroupData;
import org.vstu.compprehension.models.data.NewBankQuestionData;
import org.vstu.compprehension.models.data.QuestionMaskData;
import org.vstu.compprehension.models.data.QuestionMetadataData;
import org.vstu.compprehension.models.data.QuestionRequestLogData;
import org.vstu.compprehension.models.data.SearchIterationData;
import org.vstu.compprehension.models.data.SearchQuality;
import org.vstu.compprehension.models.repository.data.QuestionBankDataRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Банк готовых заданий: поиск подходящего вопроса и заказ генерации, когда их мало.
 * <p>
 * Работает данными: ни строк таблиц, ни репозиториев сущностей здесь нет — всё это
 * закрыто {@link QuestionBankDataRepository}. Раньше банк отдавал наружу
 * {@code QuestionMetadataEntity}, и каждый домен разворачивал её в данные сам, попутно
 * дёргая ленивую связь с телом вопроса по запросу на строку.
 */
@Log4j2
public class QuestionBank {
    private final QuestionBankDataRepository bankRepository;
    private final QuestionMetadataManager questionMetadataManager;

    public QuestionBank(QuestionBankDataRepository bankRepository) {
        this.bankRepository = bankRepository;
        this.questionMetadataManager = new QuestionMetadataManager(bankRepository);
    }

    private QuestionBankSearchRequest createBankSearchRequest(QuestionRequest qr) {
        var minComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMax();

        return QuestionBankSearchRequest.fromQuestionRequest(qr, minComplexity, maxComplexity);
    }

    public boolean isMatch(@NotNull QuestionMetadataData meta, @NotNull QuestionRequestLogData qrLog) {
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
    public boolean isMatch(@NotNull QuestionMetadataData meta, @NotNull QuestionBankSearchRequest qr) {
        // Если не совпадает имя домена – мы пытаемся сделать что-то Неправильно!
        if (qr.getDomainShortname() != null && ! qr.getDomainShortname().equalsIgnoreCase(meta.getDomainShortname())) {
            throw new RuntimeException(String.format("Trying matching a question with a QuestionRequest(Log) of different domain ! (%s != %s)", meta.getDomainShortname(), qr.getDomainShortname()));
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

        // сложность должна быть в пределах COMPLEXITY_WINDOW от запрашиваемой
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

        return true;
    }

    public int countQuestions(QuestionRequest qr) {
        var bankSearchRequest = createBankSearchRequest(qr);
        return bankRepository.countQuestions(bankSearchRequest);
    }

    public QuestionBankSearchStatsDto getStatsByQuestionRequest(QuestionRequest qr, int limit) {
        var bankSearchRequest = createBankSearchRequest(qr);
        var ordinaryCount = bankRepository.countQuestions(bankSearchRequest);
        var topRatedCount = bankRepository.countTopRatedQuestions(bankSearchRequest);
        // Тела вопросов здесь не нужны: в статистику уезжают только имя и идентификатор.
        var metadata = bankRepository.findMetadataWithoutBodies(bankSearchRequest, limit)
            .stream()
            .map(m -> new QuestionBankSearchStatsDto.QuestionMetadataDto(m.getId(), m.getName()))
            .toList();
        return new QuestionBankSearchStatsDto(ordinaryCount, topRatedCount, metadata);
    }

    public QuestionBankSearchResult searchQuestions(@NotNull QuestionRequest qr, int limit, int generatorThreshold, int generatorAdditionalQuestionsToGenerate) {

        var bankSearchRequest = createBankSearchRequest(qr);

        var prevQuestionsMasks = qr.getExerciseAttemptId() != null
            ? bankRepository.findRecentAttemptQuestionMasks(qr.getExerciseAttemptId(), 4)
            : List.<QuestionMaskData>of();

        long targetConceptsBitmaskInPlan = bankSearchRequest.getTargetConceptsBitmask();
        long targetConceptsBitmask = targetConceptsBitmaskInPlan;
        long deniedConceptsBitmask = bankSearchRequest.getDeniedConceptsBitmask();
        long unwantedConceptsBitmask = prevQuestionsMasks.stream()
                .mapToLong(QuestionMaskData::conceptBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetConceptsBitmask &= ~deniedConceptsBitmask;


        // use laws, for e.g. Expr domain
        long targetViolationsBitmaskInPlan = bankSearchRequest.getTargetLawsBitmask();
        long targetLawsBitmask = targetViolationsBitmaskInPlan;
        long deniedLawsBitmask = bankSearchRequest.getDeniedLawsBitmask();
        long unwantedLawsBitmask = prevQuestionsMasks.stream()
                .mapToLong(QuestionMaskData::lawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetLawsBitmask &= ~deniedLawsBitmask;

        // use violations from all questions is exercise attempt
        long unwantedViolationsBitmask = prevQuestionsMasks.stream()
                .mapToLong(QuestionMaskData::violationBits)
                .reduce((t, t2) -> t | t2).orElse(0);

        long targetSkillsBitmaskInPlan = bankSearchRequest.getTargetSkillsBitmask();
        long targetSkillsBitmask = targetSkillsBitmaskInPlan;
        long deniedSkillsBitmask = bankSearchRequest.getDeniedSkillsBitmask();
        long unwantedSkillsBitmask = prevQuestionsMasks.stream()
                .mapToLong(QuestionMaskData::lawBits).
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

        var searchSteps = new ArrayList<SearchIterationData>(3);
        List<QuestionMetadataData> foundQuestionMetas;

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
            log.debug("trying to do {} search with {} limit", SearchQuality.BestUnused, topRatedLimit);
            foundQuestionMetas = bankRepository.findTopRatedUnusedMetadata(preparedQuery, topRatedLimit);
            log.info("search executed with {} strategy and returns {} problems found ({} requested, {} generatorThreshold)", SearchQuality.BestUnused, foundQuestionMetas.size(), topRatedLimit, generatorThreshold);
            searchSteps.add(new SearchIterationData(SearchQuality.BestUnused, topRatedLimit, foundQuestionMetas.size()));
        }

        // runtime assert to find possible desync between findTopRatedMetadata and isMatch methods
        {
            List<Integer> notMatchedMetadata = null;
            for (QuestionMetadataData question : foundQuestionMetas) {
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
            var currentlyGeneratingQuestions = bankRepository.countCurrentlyGeneratingQuestions(qr.getDomainShortname(), preparedQuery);
            var questionsToGenerate = Math.max(1, rawQuestionsToGenerate - currentlyGeneratingQuestions);
            var genRequestId = bankRepository.createGenerationRequest(
                    preparedQuery, questionsToGenerate, qr.getExerciseAttemptId());
            log.info("created generation request with id {} with {} problems to generate", genRequestId, questionsToGenerate);
        }

        if (foundQuestionMetas.isEmpty()) {
            int normalLimit = 100;
            log.debug("trying to do {} search with {} limit", SearchQuality.Normal, normalLimit);
            foundQuestionMetas = bankRepository.findMetadata(preparedQuery, normalLimit);
            log.info("search executed with {} strategy and returns {} problems ({} requested)", SearchQuality.Normal, foundQuestionMetas.size(), normalLimit);
            searchSteps.add(new SearchIterationData(SearchQuality.Normal, normalLimit, foundQuestionMetas.size()));
        }

        if (foundQuestionMetas.isEmpty()) {
            int relaxedLimit = 100;
            log.debug("trying to do {} search with {} limit", SearchQuality.Relaxed, relaxedLimit);
            foundQuestionMetas = bankRepository.findMetadataRelaxed(preparedQuery, relaxedLimit);
            log.info("search executed with {} strategy and returns {} problems", SearchQuality.Relaxed, foundQuestionMetas.size());
            searchSteps.add(new SearchIterationData(SearchQuality.Relaxed, relaxedLimit, foundQuestionMetas.size()));
        }

        foundQuestionMetas = foundQuestionMetas.subList(0, Math.min(limit, foundQuestionMetas.size()));

        // set concepts from request (for future reference via questions' saved metadata)
        for (QuestionMetadataData m : foundQuestionMetas) {
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
        var quality = bankRepository.logSearch(preparedQuery, searchSteps, qr.getId());

        return new QuestionBankSearchResult(quality, foundQuestionMetas);
    }

    /** Вопрос банка вместе с телом; null, если такого нет или его не удалось прочитать. */
    public @Nullable QuestionMetadataData loadQuestion(int questionMetadataId) {
        try {
            var questionMeta = bankRepository.findMetadataById(questionMetadataId).orElse(null);
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
        return bankRepository.questionExists(questionName);
    }

    public long countQuestionsInDomain(String domainShortname) {
        return bankRepository.countByDomain(domainShortname);
    }

    /** Какие из перечисленных имён вопросов в банке уже заняты. */
    public Set<String> findExistingNames(String domainShortname, Collection<String> questionNames) {
        return bankRepository.findExistingNames(domainShortname, questionNames);
    }

    /** Какие из перечисленных шаблонов в банке уже заняты. */
    public Set<String> findExistingTemplateIds(String domainShortname, Collection<String> templateIds) {
        return bankRepository.findExistingTemplateIds(domainShortname, templateIds);
    }

    /** Из каких источников в банк уже брали вопросы после указанного момента. */
    public Set<String> findProcessedOrigins(String domainShortname, LocalDateTime since) {
        return bankRepository.findProcessedOrigins(domainShortname, since);
    }

    /**
     * Положить в банк сгенерированные вопросы.
     *
     * @return сколько строк метаданных записано
     */
    public int saveQuestions(List<NewBankQuestionData> questions) {
        return bankRepository.saveQuestions(questions);
    }

    /**
     * Заменить тело вопроса новым, оставив метаданные прежними.
     * <p>
     * Нужно, когда вопрос старого формата пересобирается на лету: метаданные уже
     * записаны, а тело устарело.
     */
    public void replaceQuestionBody(int metadataId, SerializableQuestion body) {
        bankRepository.replaceQuestionBody(metadataId, body);
    }

    /** Незакрытые заявки на генерацию, сгруппированные по одинаковому запросу. */
    public List<GenerationRequestGroupData> findActualGenerationRequests(
            String domainShortname, LocalDateTime createdAfter) {
        return bankRepository.findActualGenerationRequests(domainShortname, createdAfter);
    }

    /** Отметить, что по заявкам сгенерировано столько вопросов, сколько просили. */
    public void refreshGenerationRequests(Integer[] generationRequestIds) {
        bankRepository.refreshGenerationRequests(generationRequestIds);
    }

    /** Последняя заявка на генерацию, заведённая в рамках попытки. */
    public Optional<Long> findLastGenerationRequestIdOfAttempt(long exerciseAttemptId) {
        return bankRepository.findLastGenerationRequestIdOfAttempt(exerciseAttemptId);
    }
}
