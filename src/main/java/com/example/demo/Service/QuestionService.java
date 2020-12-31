package com.example.demo.Service;

import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.entities.AnswerObject;
import com.example.demo.models.entities.BackendFact;
import com.example.demo.models.repository.AnswerObjectRepository;
import com.example.demo.models.repository.BackendFactRepository;
import com.example.demo.models.repository.QuestionRepository;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }
    
    
    public void saveQuestion(Question question) {
        if (question.getAnswerObjects() != null) {
            for (AnswerObject answerObject : question.getAnswerObjects()) {
                if (answerObject.getId() == null) {
                    answerObject.setQuestion(question);
                    answerObjectRepository.save(answerObject);
                }
            }
        }
        if (question.getStatementFacts() != null) {
            for (BackendFact fact : question.getStatementFacts()) {
                if (fact.getId() == null) {
                    fact.setQuestion(question);
                    backendFactRepository.save(fact);
                }
            }
        }
        if (question.getSolutionFacts() != null) {
            for (BackendFact fact : question.getSolutionFacts()) {
                if (fact.getId() == null) {
                    fact.setQuestion(question);
                    backendFactRepository.save(fact);
                }
            }
        }
        questionRepository.save(question);
    }
    
    public com.example.demo.models.businesslogic.Question generateBusinessLogicQuestion(
            ExerciseAttempt exerciseAttempt) {
        
        //Генерируем вопрос
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Language userLanguage = exerciseAttempt.getUser().getPreferred_language();
        Domain domain = core.getDomain(
                exerciseAttempt.getExercise().getDomain().getName());
        com.example.demo.models.businesslogic.Question newQuestion = 
                domain.makeQuestion(qr, userLanguage);
        
        saveQuestion(newQuestion.getQuestionData());
        
        return newQuestion;
    }

    public com.example.demo.models.businesslogic.Question generateBusinessLogicQuestion(
            Question question) {

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

    public Question getQuestionById(Long id){

        return questionRepository.findById(id).get();

    }
}

