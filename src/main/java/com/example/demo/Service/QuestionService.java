package com.example.demo.Service;

import com.example.demo.models.businesslogic.backend.Backend;
import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.repository.AnswerObjectRepository;
import com.example.demo.models.repository.BackendFactRepository;
import com.example.demo.models.repository.QuestionRepository;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.repository.ResponseRepository;
import com.example.demo.utils.DomainAdapter;
import com.example.demo.utils.HyperText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Service
public class QuestionService {
    
    private QuestionRepository questionRepository;

    private Core core = new Core();

    @Autowired
    AnswerObjectRepository answerObjectRepository;

    @Autowired
    BackendFactRepository backendFactRepository;

    @Autowired
    private Strategy strategy;
    
    @Autowired
    private QuestionAttemptService questionAttemptService;

    @Autowired
    private Backend backend;

    @Autowired
    ResponseRepository responseRepository;
    
    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Question generateQuestion(ExerciseAttemptEntity exerciseAttempt) {
        Domain domain = DomainAdapter.getDomain(exerciseAttempt.getExercise().getDomain().getName());
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Question question = domain.makeQuestion(qr, exerciseAttempt.getUser().getPreferred_language());
        return question;
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        Domain domain = DomainAdapter.getDomain(question.getQuestionData().getDomainEntity().getName());
        List<BackendFactEntity> solution = backend.solve(
                new ArrayList<>(domain.getQuestionPositiveLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFacts(),
                domain.getSolutionVerbs(question.getQuestionDomainType(), question.getStatementFacts()));
        question.getQuestionData().setSolutionFacts(solution);
        saveQuestion(question.getQuestionData());
        return question;
    }

    public Question responseQuestion(Long questionId, List<Integer> responses) {
        Question question = getQuestion(questionId);
        for (Integer response : responses) {
            question.addResponse(makeResponse(question.getAnswerObject(response)));
        }
        return question;
    }

    public List<MistakeEntity> judgeQuestion(Question question, List<Tag> tags) {
        Domain domain = DomainAdapter.getDomain(question.getQuestionData().getDomainEntity().getName());
        List<BackendFactEntity> responseFacts = question.responseToFacts();
        List<BackendFactEntity> violations = backend.judge(
                new ArrayList<>(domain.getQuestionNegativeLaws(question.getQuestionDomainType(), tags)),
                question.getStatementFacts(),
                question.getSolutionFacts(),
                responseFacts,
                domain.getViolationVerbs(question.getQuestionDomainType(), question.getStatementFacts())
        );
        return domain.interpretSentence(violations);
    }

    public List<HyperText> explainMistakes(Question question, List<MistakeEntity> mistakes) {
        Domain domain = DomainAdapter.getDomain(question.getQuestionData().getDomainEntity().getName());
        return domain.makeExplanation(mistakes, FeedbackType.EXPLANATION);
    }

    public Question getQuestion(Long questionId) {
        Question question = generateBusinessLogicQuestion(questionRepository.findById(questionId).get());
        return question;
    }
    
    
    public void saveQuestion(QuestionEntity question) {
        questionRepository.save(question);
        if (question.getAnswerObjects() != null) {
            for (AnswerObjectEntity answerObject : question.getAnswerObjects()) {
                if (answerObject.getId() == null) {
                    answerObject.setQuestion(question);
                    answerObjectRepository.save(answerObject);
                }
            }
        }
        if (question.getStatementFacts() != null) {
            for (BackendFactEntity fact : question.getStatementFacts()) {
                if (fact.getId() == null) {
                    fact.setQuestion(question);
                    backendFactRepository.save(fact);
                }
            }
        }
        if (question.getSolutionFacts() != null) {
            for (BackendFactEntity fact : question.getSolutionFacts()) {
                if (fact.getId() == null) {
                    fact.setQuestion(question);
                    backendFactRepository.save(fact);
                }
            }
        }
    }
    
    public Question generateBusinessLogicQuestion(
            ExerciseAttemptEntity exerciseAttempt) {
        
        //Генерируем вопрос
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Language userLanguage = exerciseAttempt.getUser().getPreferred_language();
        Domain domain = core.getDomain(
                exerciseAttempt.getExercise().getDomain().getName());
        Question newQuestion =
                domain.makeQuestion(qr, userLanguage);
        
        saveQuestion(newQuestion.getQuestionData());
        
        return newQuestion;
    }

    public Question generateBusinessLogicQuestion(
            QuestionEntity question) {

        if (question.getQuestionType() == QuestionType.MATCHING) {
            return new Matching(question);
        } else if (question.getQuestionType() == QuestionType.ORDER) {
            return new Ordering(question);
        } else if (question.getQuestionType() == QuestionType.MULTI_CHOICE) {
            return new MultiChoice(question);
        } else {
            return new SingleChoice(question);
        }
    }

    private ResponseEntity makeResponse(AnswerObjectEntity answer) {
        ResponseEntity response = new ResponseEntity();
        response.setLeftAnswerObject(answer);
        response.setRightAnswerObject(answer);
        responseRepository.save(response);
        return response;
    }
}

