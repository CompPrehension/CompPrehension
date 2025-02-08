package org.vstu.compprehension.models.businesslogic.storage;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.QuestionBankSearchResultDto;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.entities.QuestionDataEntity;
import org.vstu.compprehension.models.entities.QuestionGenerationRequestEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.models.repository.QuestionDataRepository;
import org.vstu.compprehension.models.repository.QuestionGenerationRequestRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class QuestionBank {
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionDataRepository questionDataRepository;
    private final QuestionMetadataManager questionMetadataManager;
    private final QuestionGenerationRequestRepository generationRequestRepository;
    private static final float COMPLEXITY_WINDOW = 0.1f;

    public QuestionBank(
            QuestionMetadataRepository questionMetadataRepository,
            QuestionDataRepository questionDataRepository,
            QuestionGenerationRequestRepository generationRequestRepository) {
        this.questionMetadataRepository = questionMetadataRepository;
        this.questionDataRepository = questionDataRepository;
        this.questionMetadataManager = new QuestionMetadataManager(questionMetadataRepository);
        this.generationRequestRepository = generationRequestRepository;
    }

    private QuestionBankSearchRequest createBankSearchRequest(QuestionRequest qr) {
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
        if (qr.getComplexity() != 0 && Math.abs(qr.getComplexity() - meta.getIntegralComplexity()) > COMPLEXITY_WINDOW) {
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
        return questionMetadataRepository.countQuestions(bankSearchRequest, COMPLEXITY_WINDOW);
    }

    public QuestionBankSearchResultDto getStatsByQuestionRequest(QuestionRequest qr, int limit) {
        var bankSearchRequest = createBankSearchRequest(qr);
        var ordinaryCount = questionMetadataRepository.countQuestions(bankSearchRequest, COMPLEXITY_WINDOW);
        var topRatedCount = questionMetadataRepository.countTopRatedQuestions(bankSearchRequest, COMPLEXITY_WINDOW);
        var metadata = questionMetadataRepository.findMetadata(bankSearchRequest, COMPLEXITY_WINDOW, limit)
            .stream()
            .map(m -> new QuestionBankSearchResultDto.QuestionMetadataDto(m.getId(), m.getName()))
            .toList();
        return new QuestionBankSearchResultDto(ordinaryCount, topRatedCount, metadata);
    }

    public List<QuestionMetadataEntity> searchQuestions(@NotNull QuestionRequest qr, int limit) {

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

        var preparedQuery = bankSearchRequest.toBuilder()
                .targetConceptsBitmask(targetConceptsBitmask)
                .targetLawsBitmask(targetLawsBitmask)
                .targetSkillsBitmask(targetSkillsBitmask)
                .unwantedConceptsBitmask(unwantedConceptsBitmask)
                .unwantedLawsBitmask(unwantedLawsBitmask)
                .unwantedSkillsBitmask(unwantedSkillsBitmask)
                .unwantedViolationsBitmask(unwantedViolationsBitmask)
                .build();
        log.info("search query prepared: {}", new Gson().toJson(preparedQuery));
        List<QuestionMetadataEntity> foundQuestionMetas = questionMetadataRepository.findTopRatedMetadata(preparedQuery, COMPLEXITY_WINDOW, 10);
        log.info("search query executed with {} candidates", foundQuestionMetas.size());

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
        
        int generatorThreshold = 7;
        if (foundQuestionMetas.size() < generatorThreshold) {
            log.info("no enough candidates found, need additional generation");
            generationRequestRepository.save(new QuestionGenerationRequestEntity(preparedQuery, 10 - foundQuestionMetas.size(), qr.getExerciseAttemptId()));
        }
        
        if (foundQuestionMetas.isEmpty()) {
            log.info("zero candidates found, trying to do relaxed search");
            foundQuestionMetas = questionMetadataRepository.findMetadata(preparedQuery, COMPLEXITY_WINDOW, 10);
            log.info("search query executed with {} candidates", foundQuestionMetas.size());            
        }
        
        if (foundQuestionMetas.isEmpty()) {
            log.warn("no candidates found, trying to do max relaxed search");
            foundQuestionMetas = questionMetadataRepository.findMetadataRelaxed(preparedQuery, COMPLEXITY_WINDOW, 10);
            log.info("search query executed with {} candidates", foundQuestionMetas.size());
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
            m.setSkillBitsInRequest(targetSkillsBitmask);

            /// debug check law bits
            if (targetLawsBitmask != 0 && (targetLawsBitmask & m.getViolationBits()) == 0) {
                log.warn("No LAW bits matched: {} {}", targetLawsBitmask, m.getName());
            }
        }

        return foundQuestionMetas;
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

    public QuestionDataEntity saveQuestionDataEntity(QuestionDataEntity questionData) {
        return questionDataRepository.save(questionData);
    }
}
