package com.example.demo.Service;

import com.example.demo.models.Dao.QuestionDao;
import com.example.demo.models.businesslogic.*;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.models.entities.ExerciseAttempt;
import com.example.demo.models.entities.Question;
import com.example.demo.models.entities.QuestionAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {
    
    private QuestionDao questionDao;

    private Core core = new Core();

    private Strategy strategy = new Strategy();
    
    @Autowired
    private QuestionAttemptService questionAttemptService;
    
    @Autowired
    public QuestionService(QuestionDao questionDao) {
        this.questionDao = questionDao;
    }
    
    
    public void saveQuestion(Question question) {
        
        questionDao.save(question);
    }
    
    public com.example.demo.models.businesslogic.Question generateBusinessLogicQuestion(
            ExerciseAttempt exerciseAttempt) {
        
        //Генерируем вопрос
        QuestionRequest qr = strategy.generateQuestionRequest(exerciseAttempt);
        Language userLanguage = exerciseAttempt.getUser().getPreferred_language();
        com.example.demo.models.businesslogic.Domain domain = core.getDomain(
                exerciseAttempt.getExercise().getDomain().getId());
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
}

