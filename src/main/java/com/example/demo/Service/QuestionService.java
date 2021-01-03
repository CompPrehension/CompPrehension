package com.example.demo.Service;

import com.example.demo.models.businesslogic.domains.Domain;
import com.example.demo.models.entities.AnswerObjectEntity;
import com.example.demo.models.entities.BackendFactEntity;
import com.example.demo.models.repository.AnswerObjectRepository;
import com.example.demo.models.repository.BackendFactRepository;
import com.example.demo.models.repository.QuestionRepository;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.ExerciseAttemptEntity;
import com.example.demo.models.entities.QuestionEntity;
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
    
    public com.example.demo.models.businesslogic.Question generateBusinessLogicQuestion(
            ExerciseAttemptEntity exerciseAttempt) {
        
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

    public QuestionEntity getQuestionById(Long id){

        return questionRepository.findById(id).get();

    }
}

