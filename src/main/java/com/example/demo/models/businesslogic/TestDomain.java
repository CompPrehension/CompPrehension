package com.example.demo.models.businesslogic;

import com.example.demo.Service.BackendService;
import com.example.demo.Service.QuestionService;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.QuestionStatus;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.utils.HyperText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class TestDomain extends Domain {
    
    private final int DEFAULT_ANSWERS_COUNT = 4;

    public TestDomain(com.example.demo.models.entities.Domain domain) {
        super(domain);
    }

    protected ExerciseForm exerciseForm = new TestExerciseForm(domain); 
        
    @Autowired
    private BackendService backendService;
    
    @Autowired
    private QuestionService questionService;
    
    @Override
    public ExerciseForm getExerciseForm() {
        return exerciseForm;
    }

    //TODO
    @Override
    public Exercise processExerciseForm(ExerciseForm ef) {
        /*
        Exercise exercise = new Exercise();

        for (Concept c : ef.getAllConcepts()) {
            
            ExerciseConcept ec = new ExerciseConcept();
            ec.setExercise(exercise);
            ec.setConcept(c);
            
            if (ef.getDeniedConcepts().contains(c)) {
                
                exercise.
            }
        }*/
        return null;
    }

    @Override
    public Question makeQuestion(QuestionRequest questionRequest, Language userLanguage) {

        com.example.demo.models.entities.Question question = new com.example.demo.models.entities.Question();
        
        String questionText = "Вопрос на концепты: ";
        for (Concept c : questionRequest.getTargetConcepts()) {
            
            questionText += c.getName() + " ";
        }

        questionText += ". Номер правильного ответа: " + (int)(Math.random() * 4);
        
        //Создаем концепты для вопросов с выбором для стандартного бэкенда
        QuestionConceptChoice qcc = new QuestionConceptChoice();
        qcc.setBackend(backendService.getDefaultBackend());
        qcc.setQuestion(question);
        qcc.setNotSelectedConcept("notSelCon");
        qcc.setNotSelectedVerb("notSel");
        qcc.setSelectedConcept("selCon");
        qcc.setSelectedVerb("sel");
        List<QuestionConceptChoice> questionConceptChoices = new ArrayList<>();
        questionConceptChoices.add(qcc);
        
        question.setQuestionConceptChoices(questionConceptChoices);
        question.setAreAnswersRequireContext(false);
        question.setLaws(questionRequest.getTargetLaws());
        question.setQuestionStatus(QuestionStatus.VIEWED);
        question.setQuestionText(questionText);
        
        //Создаем варианты ответов
        List<AnswerObject> answerObjects = new ArrayList<>();
        for (int i = 0; i < DEFAULT_ANSWERS_COUNT; i++) {
            
            AnswerObject answerObject = new AnswerObject();
            answerObject.setRightCol(true);
            answerObject.setConcept("Concept " + i);
            answerObject.setHyperText("Concept " + i);
            answerObject.setQuestion(question);
        }
        question.setAnswerObjects(answerObjects);
        question.setQuestionType(QuestionType.SINGLE_CHOICE);
               
        
        return questionService.generateBusinessLogicQuestion(question);
    }

    @Override
    public ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType) {
        String explanation = "";
        
        if (mistakes.size() == 0) { explanation = "Ответ правильный"; } 
        else {
            
            if (feedbackType == FeedbackType.DEGREE_OF_CORRECTNESS) {
                
                explanation = "Ответ неправильный";
            } else if (feedbackType == FeedbackType.EXPLANATION) {
                
                for (Mistake m : mistakes) {
                
                    explanation += "Нарушен закон - " + m.getLaw().getName() + ";\n";
                }
                explanation += "Ответ неправильный";
            }
        }
        ArrayList<HyperText>res = new ArrayList<HyperText>() ;
        res.add(new HyperText(explanation));
        return res;
    }


}
