package org.vstu.compprehension.Service;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.data.ViolationData;
import org.vstu.compprehension.dto.AnswerDto;
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
import org.vstu.compprehension.Service.mapping.QuestionDataMapper;
import org.vstu.compprehension.models.data.QuestionData;
import org.vstu.compprehension.models.data.ResponseData;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerObjectRepository answerObjectRepository;
    private final AbstractStrategyFactory strategyFactory;
    private final DomainService domainService;
    private final InteractionRepository interactionRepository;
    private final ResponseRepository responseRepository;
    private final SupplementaryStepRepository supplementaryStepRepository;
    private final DomainFactory domainFactory;
    private final QuestionRequestLogRepository questionRequestLogRepository;
    private final QuestionBank questionStorage;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionDataMapper questionDataMapper;

    public QuestionService(QuestionRepository questionRepository, AnswerObjectRepository answerObjectRepository, AbstractStrategyFactory strategyFactory, DomainService domainService, InteractionRepository interactionRepository, ResponseRepository responseRepository, SupplementaryStepRepository supplementaryStepRepository, DomainFactory domainFactory, QuestionRequestLogRepository questionRequestLogRepository, QuestionBank questionStorage, QuestionDataMapper questionDataMapper, QuestionMetadataRepository questionMetadataRepository) {
        this.questionRepository = questionRepository;
        this.answerObjectRepository = answerObjectRepository;
        this.strategyFactory = strategyFactory;
        this.domainService = domainService;
        this.interactionRepository = interactionRepository;
        this.responseRepository = responseRepository;
        this.supplementaryStepRepository = supplementaryStepRepository;
        this.domainFactory = domainFactory;
        this.questionRequestLogRepository = questionRequestLogRepository;
        this.questionStorage = questionStorage;
        this.questionDataMapper = questionDataMapper;
        this.questionMetadataRepository = questionMetadataRepository;
    }


    public Question generateQuestion(ExerciseAttemptEntity exerciseAttempt) {
        Domain domain = domainFactory.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        AbstractStrategy strategy = strategyFactory.getStrategy(exerciseAttempt.getExercise().getStrategyId());

        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt.getId());
        qr = domain.ensureQuestionRequestValid(qr);

        Question question = domain.makeQuestion(qr, exerciseAttempt.getExercise().getOptions(),
                exerciseAttempt.getUser().getPreferred_language());

        saveQuestion(question, qr.getLogEntity(), exerciseAttempt);
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
        var question = domain.makeQuestion(questionDataMapper.toData(rawQuestion), tags, lang);
        saveQuestion(question);
        return question;
    }

    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull QuestionEntity sourceQuestion, @NotNull ViolationData violation, Language lang) {
        val domain = domainFactory.getDomain(sourceQuestion.getDomainEntity().getName());
        val responseGen = domain.makeSupplementaryQuestion(
                questionDataMapper.toData(sourceQuestion), violation, lang);
        if(responseGen.getResponse().getQuestion() != null){
            val supplementary = responseGen.getResponse().getQuestion();
            saveQuestion(supplementary, null, sourceQuestion.getExerciseAttempt());
            // Связь шага цепочки с вопросом проставляется здесь: до сохранения у вопроса
            // ещё нет идентификатора, а домену сущности недоступны.
            if (responseGen.getNewStep() != null) {
                responseGen.getNewStep().setSupplementaryQuestion(
                        getQuestionEntity(supplementary.getQuestionData().getId()));
            }
        }
        if(responseGen.getNewStep() != null){
            supplementaryStepRepository.save(responseGen.getNewStep());
        }
        return Mapper.toDto(responseGen.getResponse());
    }

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<ResponseEntity> responses) {
        Domain domain = question.getDomain();
        val supplementaryInfo = supplementaryStepRepository.findBySupplementaryQuestion(
                getQuestionEntity(question.getQuestionData().getId()));
        val feedbackGen = domain.judgeSupplementaryQuestion(question,
                supplementaryInfo == null ? null : QuestionDataMapper.toData(supplementaryInfo),
                toResponseData(responses));
        if(feedbackGen.getNewStep() != null){
            supplementaryStepRepository.save(feedbackGen.getNewStep());
        }
        return feedbackGen.getFeedback();
    }

    @SuppressWarnings("unchecked")
    public Question solveQuestion(Question question, List<Tag> tags) {
        return question.getDomain().solveQuestion(question, tags);
    }

    /*
    public List<ResponseEntity> responseQuestion(Question question, List<Integer> responses) {
        val result = new ArrayList<ResponseEntity>();
        for (val answerId : responses) {
            result.add(makeResponse(question.getAnswerObject(answerId)));
        }
        return result;
    }
    */

    public List<ResponseEntity> responseQuestion(Question question, AnswerDto[] answers) {
        val result = new ArrayList<ResponseEntity>();
        // Домен отдаёт варианты ответа данными, а ответ пишется в БД — сопоставляем
        // с сущностями по answerId.
        val answerObjectsById = getQuestionEntity(question.getQuestionData().getId())
                .getAnswerObjects().stream()
                .collect(Collectors.toMap(AnswerObjectEntity::getAnswerId, a -> a, (a, b) -> a));
        for (val answer: answers) {
            val left = answerObjectsById.get(answer.getAnswer()[0].intValue());
            val right = answerObjectsById.get(answer.getAnswer()[1].intValue());
            val createdByInteraction = Optional.ofNullable(answer.getCreatedByInteraction())
                    .flatMap(id -> interactionRepository.findById(id))
                    .orElse(null);
            val response = makeResponse(left, right, createdByInteraction);
            result.add(response);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Domain.InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        // Ответы приходят сущностями: их сохраняет сервис. Домену они нужны как данные.
        return question.getDomain().judgeQuestion(question, toResponseData(responses), tags);
    }

    /** Ответы студента в вид, с которым работают домены. */
    private List<ResponseData> toResponseData(List<ResponseEntity> responses) {
        return responses == null ? List.of()
                : responses.stream().map(response -> QuestionDataMapper.toData(response)).toList();
    }

    public Question getQuestion(Long questionId) {
        var rawQuestion = questionRepository.findByIdEager(questionId).orElseThrow();
        Question question = generateBusinessLogicQuestion(rawQuestion);
        return question;
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
        return questionRepository.findOwnerUserId(questionId);
    }

    public QuestionEntity getQuestionEntity(Long questionId) {
        return questionRepository.findById(questionId).get();
    }

    public void saveQuestion(Question question) {
        saveQuestion(question, null, null);
    }

    public void saveQuestion(Question question, @Nullable QuestionRequestLogEntity questionRequestLog) {
        saveQuestion(question, questionRequestLog, null);
    }

    /**
     * Переносит вопрос из данных в сущность и сохраняет.
     * <p>
     * Здесь собрано всё, что домены больше не делают сами, потому что для этого нужен
     * доступ к БД: привязка к домену, к попытке и к журналу запроса. Это единственный
     * сток всех путей генерации, так что пропустить привязку нельзя.
     *
     * @param questionRequestLog журнал запроса, если вопрос сгенерирован по запросу;
     *                           сохраняется раньше вопроса, связь идёт по его id
     * @param exerciseAttempt    попытка, в рамках которой задан вопрос, если она есть
     */
    public void saveQuestion(Question question,
                             @Nullable QuestionRequestLogEntity questionRequestLog,
                             @Nullable ExerciseAttemptEntity exerciseAttempt) {
        var data = question.getQuestionData();

        // Существующий вопрос обновляется на месте, новый создаётся: id есть только
        // у поднятых из БД.
        // Сущность метаданных резолвит сервис: маппер по определению не ходит в БД.
        // Метаданные приходят из банка заданий и уже существуют, поэтому берутся по id.
        var metadata = data.getMetadata() == null || data.getMetadata().getId() == null
                ? null
                : questionMetadataRepository.findById(data.getMetadata().getId()).orElse(null);

        var entity = data.getId() == null
                ? questionDataMapper.toNewEntity(data, metadata)
                : questionRepository.findById(data.getId())
                        .orElseGet(() -> questionDataMapper.toNewEntity(data, metadata));
        if (data.getId() != null) {
            questionDataMapper.applyToEntity(data, entity, metadata);
        }

        if (questionRequestLog != null) {
            questionRequestLogRepository.save(questionRequestLog);
            entity.setQuestionRequestLog(questionRequestLog);
        }
        if (exerciseAttempt != null) {
            entity.setExerciseAttempt(exerciseAttempt);
        }
        if (entity.getDomainEntity() == null) {
            entity.setDomainEntity(domainService.getDomainEntity(question.getDomain().getName()));
        }

        questionRepository.save(entity);

        // Варианты ответа сохраняются после вопроса: у новых связь идёт по его id.
        if (entity.getAnswerObjects() != null) {
            for (AnswerObjectEntity answerObject : entity.getAnswerObjects()) {
                if (answerObject.getQuestion() == null) {
                    answerObject.setQuestion(entity);
                }
            }
            answerObjectRepository.saveAll(entity.getAnswerObjects().stream().filter(a -> a.getId() == null)::iterator);
        }

        // Идентификаторы, назначенные базой, возвращаются в данные: вызывающий код
        // продолжает работать с тем же объектом вопроса.
        data.setId(entity.getId());
        for (int i = 0; i < entity.getAnswerObjects().size() && i < data.getAnswerObjects().size(); i++) {
            data.getAnswerObjects().get(i).setId(entity.getAnswerObjects().get(i).getId());
        }
    }

    public Domain.CorrectAnswer getNextCorrectAnswer(Question question) {
        return question.getDomain().getAnyNextCorrectAnswer(question);
    }

    /*
    public Question generateBusinessLogicQuestion(ExerciseAttemptEntity exerciseAttempt) {
        
        //Генерируем вопрос
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt.getId());
        Language userLanguage = exerciseAttempt.getUser().getPreferred_language();
        Domain domain = core.getDomain(
                exerciseAttempt.getExercise().getDomain().getName());
        Question newQuestion =
                domain.makeQuestion(qr, exerciseAttempt.getExercise().getTags(), userLanguage);
        
        saveQuestion(newQuestion.getQuestionData());
        
        return newQuestion;
    }
    */

    public Question generateBusinessLogicQuestion(QuestionEntity question) {
        Domain domain = domainFactory.getDomain(question.getDomainEntity().getName());
        return new Question(questionDataMapper.toData(question), domain);
    }

    private ResponseEntity makeResponse(AnswerObjectEntity answer) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answer);
        response.setRightAnswerObject(answer);
        responseRepository.save(response);
        return response;
    }

    private ResponseEntity makeResponse(AnswerObjectEntity answerL, AnswerObjectEntity answerR, InteractionEntity createdByInteraction) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answerL);
        response.setRightAnswerObject(answerR);
        response.setCreatedByInteraction(createdByInteraction);
        responseRepository.save(response);
        return response;
    }
}
