package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.frontend.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.businesslogic.Question;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.data.question.InteractionResponsesData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.QuestionRequestLogData;
import org.vstu.compprehension.data.question.RecordedInteractionData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.repositories.data.InteractionDataRepository;
import org.vstu.compprehension.repositories.data.QuestionDataRepository;
import org.vstu.compprehension.repositories.data.SupplementaryStepDataRepository;
import org.vstu.compprehension.frontend.mappers.SupplementaryQuestionDtoMapper;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
class QuestionDataServiceImpl implements QuestionDataService {
    private final AbstractStrategyFactory strategyFactory;
    private final SupplementaryStepDataRepository supplementaryStepDataRepository;
    private final QuestionDataRepository questionDataRepository;
    private final InteractionDataRepository interactionDataRepository;
    private final ExerciseAttemptDataService exerciseAttemptService;
    private final DomainFactory domainFactory;
    private final QuestionBank questionStorage;
    private final SupplementaryQuestionDtoMapper supplementaryQuestionDtoMapper;


    public Question generateQuestion(long exerciseAttemptId) {
        var context = exerciseAttemptService.getGenerationContext(exerciseAttemptId);
        Domain domain = domainFactory.getDomain(context.domainId());
        AbstractStrategy strategy = strategyFactory.getStrategy(context.strategyId());

        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttemptId);
        qr = domain.ensureQuestionRequestValid(qr);

        Question question = domain.makeQuestion(qr, context.exerciseOptions(), context.userLanguage());

        saveQuestion(question, qr.toLogData(), exerciseAttemptId);
        return question;
    }

    public Question generateQuestion(int questionMetadataId, Language lang) {
        var rawQuestion = questionStorage.loadQuestion(questionMetadataId);
        if (rawQuestion == null) {
            throw new RuntimeException("Metadata with id " + questionMetadataId + " not found");
        }
        var domain = domainFactory.getDomain(rawQuestion.getDomainShortname());
        var tags = domain.getAllTags().stream()
                .filter(t -> rawQuestion.getTagBits() != null && (rawQuestion.getTagBits() & t.getBitmask()) != 0)
                .toList();
        var question = domain.makeQuestion(rawQuestion, tags, lang);
        saveQuestion(question);
        return question;
    }

    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(long sourceQuestionId, @NotNull ViolationData violation, Language lang) {
        val domain = domainFactory.getDomain(getDomainName(sourceQuestionId));
        val responseGen = domain.makeSupplementaryQuestion(
                questionDataRepository.findById(sourceQuestionId), violation, lang);

        Long supplementaryQuestionId = null;
        if(responseGen.getResponse().getQuestion() != null){
            val supplementary = responseGen.getResponse().getQuestion();
            saveQuestion(supplementary, null, exerciseAttemptService.findAttemptIdOfQuestion(sourceQuestionId).orElse(null));
            supplementaryQuestionId = supplementary.getQuestionData().getId();
        }
        if(responseGen.getNewStep() != null){
            supplementaryStepDataRepository.create(responseGen.getNewStep(), supplementaryQuestionId);
        }
        return supplementaryQuestionDtoMapper.map(responseGen.getResponse(), lang);
    }

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<? extends AnswerData> responses, Language language) {
        Domain domain = question.getDomain();
        val supplementaryInfo = supplementaryStepDataRepository
                .findBySupplementaryQuestionId(question.getQuestionData().getId());
        val feedbackGen = domain.judgeSupplementaryQuestion(question, supplementaryInfo, responses, language);
        if(feedbackGen.getNewStep() != null){
            supplementaryStepDataRepository.create(feedbackGen.getNewStep(), null);
        }
        return feedbackGen.getFeedback();
    }

    public List<AnswerData> resolveAnswers(long questionId, List<SubmittedAnswerData> answers) {
        return interactionDataRepository.resolveAnswers(questionId, answers);
    }

    /** Ответы последнего взаимодействия, после которого вопрос ещё можно продолжать. */
    public Optional<InteractionResponsesData> findLatestCorrectInteraction(long questionId) {
        return interactionDataRepository.findLatestCorrectInteraction(questionId);
    }

    /**
     * Записать взаимодействие студента с вопросом.
     */
    public RecordedInteractionData recordInteraction(NewInteractionData interaction) {
        return interactionDataRepository.record(interaction);
    }

    /** Выставить оценку за уже записанное взаимодействие. */
    public void gradeInteraction(long interactionId, float grade) {
        interactionDataRepository.grade(interactionId, grade);
    }

    public Question getQuestion(Long questionId) {
        return new Question(
                questionDataRepository.findById(questionId),
                domainFactory.getDomain(getDomainName(questionId)));
    }

    /** Имя домена вопроса — скалярным запросом, без подъёма всего вопроса. */
    private @NotNull String getDomainName(long questionId) {
        return questionDataRepository.getDomainName(questionId);
    }

    public Question getSolvedQuestion(Long questionId) {
        val question = getQuestion(questionId);
        var tags = question.getTags();
        var domain = question.getDomain();
        val solved = domain.solveQuestion(question, tags);

        // Решение дописывает в вопрос факты (FactBackend.updateQuestionAfterSolve),
        // и до этой строки они сохранялись неявно: вопрос загружен из БД, значит
        // управляется Hibernate, и изменение уезжало в базу при коммите транзакции.
        // Запись сделана явной, потому что как только вопрос станет отсоединённым
        // контейнером (QuestionData), dirty checking перестанет работать и факты
        // решения молча пропадут. Сейчас, при живом dirty checking, это no-op:
        // та же строка, та же транзакция.
        saveQuestion(solved);

        return solved;
    }

    /**
     * Id пользователя, которому принадлежит попытка, породившая вопрос.
     */
    public Optional<Long> findQuestionOwnerUserId(Long questionId) {
        return questionDataRepository.findOwnerUserId(questionId);
    }

    public void saveQuestion(Question question,
                             @Nullable QuestionRequestLogData questionRequestLog,
                             @Nullable Long exerciseAttemptId) {
        questionDataRepository.save(question.getQuestionData(), question.getDomain().getName(),
                questionRequestLog, exerciseAttemptId);
    }

    private void saveQuestion(Question question) {
        saveQuestion(question, null, null);
    }
}
