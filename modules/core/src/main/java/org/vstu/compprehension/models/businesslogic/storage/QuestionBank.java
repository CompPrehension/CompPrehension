package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.repository.QuestionDataRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.RandomProvider;

import java.util.*;

@Log4j2
public class QuestionBank {
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionDataRepository questionDataRepository;
    private final QuestionMetadataManager questionMetadataManager;
    private final RandomProvider randomProvider;

    public QuestionBank(
            QuestionMetadataRepository questionMetadataRepository,
            QuestionDataRepository questionDataRepository,
            QuestionMetadataManager questionMetadataManager, RandomProvider randomProvider) {
        this.questionMetadataRepository = questionMetadataRepository;
        this.questionDataRepository = questionDataRepository;
        this.questionMetadataManager = questionMetadataManager;
        this.randomProvider = randomProvider;
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

    private boolean isMatch(@NotNull QuestionMetadataEntity meta, @NotNull QuestionBankSearchRequest qr) {
        // Если не совпадает имя домена – мы пытаемся сделать что-то Неправильно!
        if (qr.getDomainShortname() != null && ! qr.getDomainShortname().equalsIgnoreCase(meta.getDomainShortname())) {
            throw new RuntimeException(String.format("Trying matching a question with a QuestionRequest(LogEntity) of different domain ! (%s != %s)", meta.getDomainShortname(), qr.getDomainShortname()));
        }

        // проверка запрещаемых критериев
        if (meta.getSolutionSteps() < qr.getStepsMin()
                || qr.getStepsMax() != 0 && meta.getSolutionSteps() > qr.getStepsMax()
                || qr.getDeniedConceptsBitmask() != 0 && (meta.getConceptBits() & qr.getDeniedConceptsBitmask()) != 0
                || qr.getDeniedLawsBitmask() != 0 && (meta.getViolationBits() & qr.getDeniedLawsBitmask()) != 0
                || qr.getTargetTagsBitmask() != 0 && (meta.getTagBits() & qr.getTargetTagsBitmask()) != meta.getTagBits() // требуем наличия всех тэгов
        ) {
            return false;
        }

        // Если есть запрет по ID шаблона или имени вопроса
        if (qr.getDeniedQuestionTemplateIds() != null && !qr.getDeniedQuestionTemplateIds().isEmpty() && qr.getDeniedQuestionTemplateIds().contains(meta.getTemplateId())
                || qr.getDeniedQuestionNames() != null && !qr.getDeniedQuestionNames().isEmpty() && qr.getDeniedQuestionNames().contains(meta.getName())) {
            return false;
        }

        // сложность должна быть <= от запрашиваемой
        if (qr.getComplexity() != 0 && meta.getIntegralComplexity() > qr.getComplexity()) {
            return false;
        }

        // Присутствует хотя бы половина целевых концептов и законов
        if (qr.getTargetConceptsBitmask() != 0 && ((meta.getConceptBits() & qr.getTargetConceptsBitmask()) == 0 || Long.bitCount(meta.getConceptBits() & qr.getTargetConceptsBitmask()) < Long.bitCount(qr.getTargetConceptsBitmask()) / 2)
                || qr.getTargetLawsBitmask() != 0 && ((meta.getLawBits() & qr.getTargetLawsBitmask()) == 0 || Long.bitCount(meta.getLawBits() & qr.getTargetLawsBitmask()) < Long.bitCount(qr.getTargetLawsBitmask()) / 2)
        ) {
            return false;
        }

        return true;
    }

    public int countQuestions(QuestionRequest qr) {
        var bankSearchRequest = createBankSearchRequest(qr);
        return questionMetadataRepository.countQuestions(bankSearchRequest);
    }

    /**
     * Find question templates in the questions bank. If no questions satisfy the requirements exactly, finds ones of similar complexity. Denied concepts and laws (laws map to violations on DB) cannot present in result questions anyway.
     * @param qr QuestionRequest
     * @param limit maximum questions to return (must be > 0)
     * @return questions found or empty list if the requirements cannot be satisfied
     */
    public List<Question> searchQuestions(Domain domain, ExerciseAttemptEntity attempt, QuestionRequest qr, int limit) {

        var bankSearchRequest = createBankSearchRequest(qr);
        
        var prevQuestionsMetadata = questionMetadataRepository.findLastNExerciseAttemptMeta(attempt.getId(), 4);

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
        long unwantedViolationsBitmask = attempt.getQuestions().stream()
                .map(QuestionEntity::getMetadata)
                .filter(Objects::nonNull)
                .mapToLong(QuestionMetadataEntity::getViolationBits)
                .reduce((t, t2) -> t | t2).orElse(0);

        var preparedQuery = bankSearchRequest.toBuilder()
                .targetConceptsBitmask(targetConceptsBitmask)
                .targetLawsBitmask(targetLawsBitmask)
                .unwantedConceptsBitmask(unwantedConceptsBitmask)
                .unwantedLawsBitmask(unwantedLawsBitmask)
                .unwantedViolationsBitmask(unwantedViolationsBitmask)
                .build();
        log.debug("search query prepared: {}", preparedQuery);
        List<QuestionMetadataEntity> foundQuestionMetas = questionMetadataRepository.findTopRatedMetadata(preparedQuery, 10);
        log.debug("search query executed with {} candidates", foundQuestionMetas.size());
        
        if (foundQuestionMetas.size() < 7) {
            // TODO send generation request
            log.info("no enough candidates found, need additional generation");
        }
        
        if (foundQuestionMetas.isEmpty()) {
            log.debug("zero candidates found, trying to do relaxed search");
            foundQuestionMetas = questionMetadataRepository.findMetadata(preparedQuery, 10);
            log.debug("search query executed with {} candidates", foundQuestionMetas.size());            
        }
        
        if (foundQuestionMetas.isEmpty()) {
            log.warn("no candidates found");
            return Collections.emptyList();
        }

        foundQuestionMetas = foundQuestionMetas.subList(0, Math.min(limit, foundQuestionMetas.size()));
        
        // set concepts from request (for future reference via questions' saved metadata)
        for (QuestionMetadataEntity m : foundQuestionMetas) {
            m.setConceptBitsInPlan(targetConceptsBitmaskInPlan);
            m.setViolationBitsInPlan(targetViolationsBitmaskInPlan);
            // Save actual requested bits as well
            m.setConceptBitsInRequest(targetConceptsBitmask);
            m.setViolationBitsInRequest(targetLawsBitmask);

            /// debug check law bits
            if (targetLawsBitmask != 0 && (targetLawsBitmask & m.getViolationBits()) == 0) {
                log.warn("No LAW bits matched: {} {}", targetLawsBitmask, m.getName());
            }
        }

        List<Question> loadedQuestions = loadQuestions(domain, foundQuestionMetas);
        log.info("{} questions loaded", loadedQuestions.size());

        return loadedQuestions;
    }

    private List<Question> loadQuestions(Domain domain, Collection<QuestionMetadataEntity> metas) {
        List<Question> list = new ArrayList<>();
        for (QuestionMetadataEntity meta : metas) {
            Question question = loadQuestion(domain, meta);
            if (question != null) {
                list.add(question);
            }
        }
        return list;
    }

    private @Nullable Question loadQuestion(Domain domain, @NotNull QuestionMetadataEntity qMeta) {
        try {
            var questionData = qMeta.getQuestionData();
            if (questionData != null) {
                return questionData.getData().toQuestion(domain, qMeta);
            }

            QuestionDataEntity questionDataEntity = questionDataRepository.findById(qMeta.getId()).orElse(null);
            if (questionDataEntity != null) {
                return questionDataEntity.getData().toQuestion(domain, qMeta);
            }

            log.warn("Question data NOT found for metadata id: {}", qMeta.getId());
        } catch (Exception e) {
            log.error("Error loading question with metadata id [{}] - {}", qMeta.getId(), e.getMessage(), e);
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
