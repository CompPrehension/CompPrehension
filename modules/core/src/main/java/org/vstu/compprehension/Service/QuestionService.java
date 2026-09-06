package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.data.ViolationData;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.data.InteractionResponsesData;
import org.vstu.compprehension.models.data.NewInteractionData;
import org.vstu.compprehension.models.data.QuestionData;
import org.vstu.compprehension.models.data.QuestionRequestLogData;
import org.vstu.compprehension.models.data.RecordedInteractionData;
import org.vstu.compprehension.models.data.ResponseData;
import org.vstu.compprehension.models.data.SubmittedAnswerData;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.repository.data.InteractionDataRepository;
import org.vstu.compprehension.models.repository.data.QuestionDataRepository;
import org.vstu.compprehension.models.repository.data.SupplementaryStepDataRepository;
import org.vstu.compprehension.utils.Mapper;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class QuestionService {
    private final AbstractStrategyFactory strategyFactory;
    private final SupplementaryStepDataRepository supplementaryStepDataRepository;
    private final QuestionDataRepository questionDataRepository;
    private final InteractionDataRepository interactionDataRepository;
    private final ExerciseAttemptService exerciseAttemptService;
    private final DomainFactory domainFactory;
    private final QuestionBank questionStorage;


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

        // Связь шага цепочки со сгенерированным вопросом проставляется здесь: до
        // сохранения у вопроса ещё нет идентификатора, а домену он недоступен.
        Long supplementaryQuestionId = null;
        if(responseGen.getResponse().getQuestion() != null){
            val supplementary = responseGen.getResponse().getQuestion();
            saveQuestion(supplementary, null,
                    exerciseAttemptService.findAttemptIdOfQuestion(sourceQuestionId).orElse(null));
            supplementaryQuestionId = supplementary.getQuestionData().getId();
        }
        if(responseGen.getNewStep() != null){
            supplementaryStepDataRepository.create(responseGen.getNewStep(), supplementaryQuestionId);
        }
        return Mapper.toDto(responseGen.getResponse());
    }

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<ResponseData> responses) {
        Domain domain = question.getDomain();
        val supplementaryInfo = supplementaryStepDataRepository
                .findBySupplementaryQuestionId(question.getQuestionData().getId());
        val feedbackGen = domain.judgeSupplementaryQuestion(question, supplementaryInfo, responses);
        if(feedbackGen.getNewStep() != null){
            supplementaryStepDataRepository.create(feedbackGen.getNewStep(), null);
        }
        return feedbackGen.getFeedback();
    }

    @SuppressWarnings("unchecked")
    public Question solveQuestion(Question question, List<Tag> tags) {
        return question.getDomain().solveQuestion(question, tags);
    }

    /**
     * Ответы, пришедшие с фронта, в вид, с которым работают домены.
     * <p>
     * В БД при этом ничего не пишется: ответ становится строкой только вместе со
     * взаимодействием, которое его объясняет.
     */
    public List<ResponseData> resolveAnswers(long questionId, List<SubmittedAnswerData> answers) {
        return interactionDataRepository.resolveAnswers(questionId, answers);
    }

    /** Ответы последнего взаимодействия, после которого вопрос ещё можно продолжать. */
    public Optional<InteractionResponsesData> findLatestCorrectInteraction(long questionId) {
        return interactionDataRepository.findLatestCorrectInteraction(questionId);
    }

    /**
     * Записать взаимодействие студента с вопросом.
     * <p>
     * Оценка выставляется отдельно: её считает стратегия по истории попытки, в которую
     * входит и это взаимодействие.
     */
    public RecordedInteractionData recordInteraction(NewInteractionData interaction) {
        return interactionDataRepository.record(interaction);
    }

    /** Выставить оценку за уже записанное взаимодействие. */
    public void gradeInteraction(long interactionId, float grade) {
        interactionDataRepository.grade(interactionId, grade);
    }

    @SuppressWarnings("unchecked")
    public Domain.InterpretSentenceResult judgeQuestion(Question question, List<ResponseData> responses, List<Tag> tags) {
        return question.getDomain().judgeQuestion(question, responses, tags);
    }

    public Question getQuestion(Long questionId) {
        return new Question(questionDataRepository.findById(questionId),
                domainFactory.getDomain(getDomainName(questionId)));
    }

    /** Имя домена вопроса — скалярным запросом, без подъёма всего вопроса. */
    private @NotNull String getDomainName(long questionId) {
        return questionDataRepository.getDomainName(questionId);
    }

    public Question getSolvedQuestion(Long questionId) {
        val question = getQuestion(questionId);
        var tags = question.getTags();
        val solved = solveQuestion(question, tags);

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

    public void saveQuestion(Question question) {
        saveQuestion(question, null, null);
    }

    public void saveQuestion(Question question, @Nullable QuestionRequestLogData questionRequestLog) {
        saveQuestion(question, questionRequestLog, null);
    }

    /**
     * Записать вопрос.
     * <p>
     * Домен, попытка и журнал запроса в самих данных вопроса не лежат, поэтому
     * передаются рядом: это единственный сток всех путей генерации, и пропустить
     * привязку нельзя.
     *
     * @param questionRequestLog журнал запроса, если вопрос сгенерирован по запросу
     * @param exerciseAttemptId  попытка, в рамках которой задан вопрос, если она есть
     */
    public void saveQuestion(Question question,
                             @Nullable QuestionRequestLogData questionRequestLog,
                             @Nullable Long exerciseAttemptId) {
        questionDataRepository.save(question.getQuestionData(), question.getDomain().getName(),
                questionRequestLog, exerciseAttemptId);
    }

    public Domain.CorrectAnswer getNextCorrectAnswer(Question question) {
        return question.getDomain().getAnyNextCorrectAnswer(question);
    }

}
