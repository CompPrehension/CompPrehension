package org.vstu.compprehension.Service;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.AnswerDto;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.models.businesslogic.DomainToBackendAdapter;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.BackendFactory;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;
    private final AnswerObjectRepository answerObjectRepository;
    private final AbstractStrategyFactory strategyFactory;
    private final BackendFactory backendFactory;
    private final DomainService domainService;
    private final InteractionRepository interactionRepository;
    private final ResponseRepository responseRepository;
    private final SupplementaryStepRepository supplementaryStepRepository;
    private final DomainFactory domainFactory;
    private final QuestionRequestLogRepository questionRequestLogRepository;
    private final QuestionBank questionStorage;

    public QuestionService(QuestionRepository questionRepository, AnswerObjectRepository answerObjectRepository, AbstractStrategyFactory strategyFactory, BackendFactory backendFactory, DomainService domainService, InteractionRepository interactionRepository, ResponseRepository responseRepository, SupplementaryStepRepository supplementaryStepRepository, DomainFactory domainFactory, QuestionRequestLogRepository questionRequestLogRepository, QuestionBank questionStorage) {
        this.questionRepository = questionRepository;
        this.answerObjectRepository = answerObjectRepository;
        this.strategyFactory = strategyFactory;
        this.backendFactory = backendFactory;
        this.domainService = domainService;
        this.interactionRepository = interactionRepository;
        this.responseRepository = responseRepository;
        this.supplementaryStepRepository = supplementaryStepRepository;
        this.domainFactory = domainFactory;
        this.questionRequestLogRepository = questionRequestLogRepository;
        this.questionStorage = questionStorage;
    }


    public Question generateQuestion(ExerciseAttemptEntity exerciseAttempt) {
        Domain domain = domainFactory.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        AbstractStrategy strategy = strategyFactory.getStrategy(exerciseAttempt.getExercise().getStrategyId());

        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        qr = domain.ensureQuestionRequestValid(qr);

        Question question = domain.makeQuestion(qr, exerciseAttempt, exerciseAttempt.getUser().getPreferred_language());
        question.setQuestionRequest(createQuestionRequestLog(qr));

        saveQuestion(question);
        return question;
    }

    public Question generateQuestion(int questionMetadataId) {
        var rawQuestion = questionStorage.loadQuestion(questionMetadataId);
        if (rawQuestion == null) {
            throw new RuntimeException("Metadata with id " + questionMetadataId + " not found");
        }
        var domain = domainFactory.getDomain(rawQuestion.getDomainShortname());
        var tags = domain.getAllTags().stream()
                .filter(t -> rawQuestion.getTagBits() != null && (rawQuestion.getTagBits() & t.getBitmask()) != 0)
                .toList();
        var question = domain.makeQuestion(rawQuestion, null, tags, Language.ENGLISH);
        saveQuestion(question);
        return question;
    }

    private QuestionRequestLogEntity createQuestionRequestLog(QuestionRequest qr) {
        int questionsFound = questionStorage.countQuestions(qr);
        return qr.getLogEntity(questionsFound);
    }

    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull QuestionEntity sourceQuestion, @NotNull ViolationEntity violation, Language lang) {
        val domain = domainFactory.getDomain(sourceQuestion.getDomainEntity().getName());
        val responseGen = domain.makeSupplementaryQuestion(sourceQuestion, violation, lang);
        if(responseGen.getResponse().getQuestion() != null){
            Question question = responseGen.getResponse().getQuestion();
            question.getQuestionData().setDomainEntity(domainService.getDomainEntity(domain.getName()));
            saveQuestion(question);
        }
        if(responseGen.getNewStep() != null){
            supplementaryStepRepository.save(responseGen.getNewStep());
        }
        return Mapper.toDto(responseGen.getResponse());
    }

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<ResponseEntity> responses) {
        Domain domain = question.getDomain();
        val supplementaryInfo = supplementaryStepRepository.findBySupplementaryQuestion(question.getQuestionData());
        val feedbackGen = domain.judgeSupplementaryQuestion(question, supplementaryInfo, responses);
        if(feedbackGen.getNewStep() != null){
            supplementaryStepRepository.save(feedbackGen.getNewStep());
        }
        return feedbackGen.getFeedback();
    }

    @SuppressWarnings("unchecked")
    public Question solveQuestion(Question question, List<Tag> tags) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Backend backend = backendFactory.getBackend(
                domain.getJudgingBackendId()
        );
        DomainToBackendAdapter usedInterface = domain.getBackendInterface(domain.getSolvingBackendId());

        usedInterface.updateQuestionAfterSolve(
            question,
            backend.solve(usedInterface.prepareBackendInfoForSolve(question, tags))
        );

        return question;
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
        for (val answer: answers) {
            val left = question.getAnswerObject(answer.getAnswer()[0].intValue());
            val right = question.getAnswerObject(answer.getAnswer()[1].intValue());
            val createdByInteraction = Optional.ofNullable(answer.getCreatedByInteraction())
                    .flatMap(id -> interactionRepository.findById(id))
                    .orElse(null);
            val response = makeResponse(left, right, createdByInteraction);
            result.add(response);
        }
        return result;
    }

    /**
     * @param question current question being solved
     * @param responses new responses from student (to add to solution if correct)
     * @param tags Exercise tags
     * @return interpretation of backend's judgement
     */
    @SuppressWarnings("unchecked")
    public Domain.InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Backend backend = backendFactory.getBackend(
            domain.getJudgingBackendId()
        );
        DomainToBackendAdapter usedInterface = domain.getBackendInterface(domain.getJudgingBackendId());

        Object output = backend.judge(usedInterface.prepareBackendInfoForJudge(question, responses, tags));
        return usedInterface.interpretJudgeOutput(question, output);
    }

    public Question getQuestion(Long questionId) {
        var rawQuestion = questionRepository.findByIdEager(questionId).orElseThrow();
        Question question = generateBusinessLogicQuestion(rawQuestion);
        return question;
    }

    public Question getSolvedQuestion(Long questionId) {
        val question = getQuestion(questionId);
        var tags = question.getTags();
        return solveQuestion(question, tags);
    }

    public QuestionEntity getQuestionEntity(Long questionId) {
        return questionRepository.findById(questionId).get();
    }

    public void saveQuestion(Question question) {
        var questionData = question.getQuestionData();

        if (questionData.getAnswerObjects() != null) {
            for (AnswerObjectEntity answerObject : question.getAnswerObjects()) {
                if (answerObject.getId() == null) {
                    answerObject.setQuestion(questionData);
                }
            }
            answerObjectRepository.saveAll(questionData.getAnswerObjects().stream().filter(a -> a.getId() == null)::iterator);
        }

        if (question.getQuestionRequest() != null) {
            questionRequestLogRepository.save(question.getQuestionRequest());
        }

        for (val interactionEntity : questionData.getInteractions()) {
            if (interactionEntity.getQuestion() == null) {
                interactionEntity.setQuestion(questionData);
            }
        }

        questionRepository.save(questionData);
    }

    public Domain.CorrectAnswer getNextCorrectAnswer(Question question) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        return domain.getAnyNextCorrectAnswer(question);
    }

    /*
    public Question generateBusinessLogicQuestion(ExerciseAttemptEntity exerciseAttempt) {
        
        //Генерируем вопрос
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
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
        return new Question(question, domain);
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

