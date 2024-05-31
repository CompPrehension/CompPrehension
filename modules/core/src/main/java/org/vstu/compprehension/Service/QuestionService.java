package org.vstu.compprehension.Service;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.AnswerDto;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.dto.SupplementaryQuestionDto;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.Backend;
import org.vstu.compprehension.models.businesslogic.backend.BackendFactory;
import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.backend.util.ReasoningOptions;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategyFactory;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.repository.*;
import org.vstu.compprehension.utils.HyperText;
import org.vstu.compprehension.utils.Mapper;

import java.util.*;

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

    @Autowired
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
        
        var tags = exerciseAttempt.getExercise().getTags().stream().map(domain::getTag).filter(Objects::nonNull).toList();
        Question question = domain.makeQuestion(exerciseAttempt, qr, tags, exerciseAttempt.getUser().getPreferred_language());
        question.setQuestionRequest(createQuestionRequestLog(qr));
        
        saveQuestion(question);
        return question;
    }

    private QuestionRequestLogEntity createQuestionRequestLog(QuestionRequest qr) {
        int questionsFound = questionStorage.countQuestions(qr);
        return qr.getLogEntity(questionsFound);
    }

    public @NotNull SupplementaryQuestionDto generateSupplementaryQuestion(@NotNull QuestionEntity sourceQuestion, @NotNull ViolationEntity violation, Language lang) {
        val domain = domainFactory.getDomain(sourceQuestion.getExerciseAttempt().getExercise().getDomain().getName());
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

    public SupplementaryFeedbackDto judgeSupplementaryQuestion(Question question, List<ResponseEntity> responses, ExerciseAttemptEntity exerciseAttempt) {
        Domain domain = domainFactory.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        val supplementaryInfo = supplementaryStepRepository.findBySupplementaryQuestion(question.getQuestionData());
        val feedbackGen = domain.judgeSupplementaryQuestion(question, supplementaryInfo, responses);
        if(feedbackGen.getNewStep() != null){
            supplementaryStepRepository.save(feedbackGen.getNewStep());
        }
        return feedbackGen.getFeedback();
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Backend backend = backendFactory.getBackend(question.getQuestionData().getExerciseAttempt().getExercise().getBackendId());

        // use reasoner to solve question
        Collection<Fact> solution = backend.solve(
                /*new ArrayList<>*/(domain.getQuestionLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFactsWithSchema(),
                new ReasoningOptions(
                        false,
                        domain.getSolutionVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
                        question.getQuestionUniqueTemplateName()
                ));

        List<BackendFactEntity> storedSolution = question.getQuestionData().getSolutionFacts();
        if (storedSolution != null && !storedSolution.isEmpty()) {
            // add anything set as solution before
            solution.addAll(Fact.entitiesToFacts(storedSolution));
        }
        // save facts to question
        question.getQuestionData().setSolutionFacts(Fact.factsToEntities(solution));

        // don't save solution into DB
        // // saveQuestion(question.getQuestionData());

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

    public Domain.InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Collection<Fact> responseFacts = question.responseToFacts(responses);
        Backend backend = backendFactory.getBackend(question.getQuestionData().getExerciseAttempt().getExercise().getBackendId());
        Collection<Fact> violations = backend.judge(
                new ArrayList<>(domain.getQuestionNegativeLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFactsWithSchema(),
                Fact.entitiesToFacts(question.getSolutionFacts()),
                responseFacts,
                new ReasoningOptions(
                        false,
                        domain.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts()),
                        question.getQuestionUniqueTemplateName())
        );
        return domain.interpretSentence(violations);
    }

    public List<HyperText> explainViolations(Question question, List<ViolationEntity> violations) {
        Domain domain = domainFactory.getDomain(question.getQuestionData().getDomainEntity().getName());
        Language lang;
        try {
            lang = question.getQuestionData().getExerciseAttempt().getUser().getPreferred_language(); // The language currently selected in UI
        } catch (NullPointerException e) {
            lang = Language.ENGLISH;  // fallback if it cannot be figured out
        }
        return domain.makeExplanation(violations, FeedbackType.EXPLANATION, lang);
    }

    public Question getQuestion(Long questionId) {
        var rawQuestion = questionRepository.findByIdEager(questionId).orElseThrow();
        Question question = generateBusinessLogicQuestion(rawQuestion);
        return question;
    }

    public Question getSolvedQuestion(Long questionId) {
        val question = getQuestion(questionId);
        var tags = question.getQuestionData().getExerciseAttempt().getExercise().getTags()
                .stream().map(t -> question.getDomain().getTag(t)).filter(Objects::nonNull).toList();
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
        Domain domain = domainFactory.getDomain(question.getExerciseAttempt().getExercise().getDomain().getName());
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

