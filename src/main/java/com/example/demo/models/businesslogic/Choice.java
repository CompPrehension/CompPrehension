package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.BackendFact;
import com.example.demo.models.entities.AnswerObject;
import com.example.demo.models.entities.Response;

import java.util.ArrayList;
import java.util.List;

public class Choice extends Question {

    public Choice(com.example.demo.models.entities.Question questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFact> responseToFacts() {

        List<AnswerObject> answers = new ArrayList<>(super.
                getAnswerObjects());
        List<Response> responses = super.studentResponses;
        List<BackendFact> facts = new ArrayList<>();
//        QuestionConceptChoice questionConcept = questionData.
//                getQuestionConceptChoices().get(0);
//        //Формируем факты из ответов студент
//        for (Response r : responses) {
//            //Формируем элементы триплета
//            String object = questionConcept.getSelectedConcept();
//            String subject = r.getRightAnswerObject().getConcept();
//            String verb = questionConcept.getSelectedVerb();
//            //Удаляем вариант ответа, т.к. на основе него уже сформирован факт
//            answers.remove(r.getRightAnswerObject());
//            //Создаем на основе триплета факт
//            BackendFact fact = new BackendFact(object, subject, verb);
//            facts.add(fact);
//        }
//        //Формируем факты из невыбранных ответов
//        for (AnswerObject ao : answers) {
//            //Формируем элементы триплета
//            String object = questionConcept.getNotSelectedConcept();
//            String subject = ao.getConcept();
//            String verb = questionConcept.getNotSelectedVerb();
//            //Создаем на основе триплета факт
//            BackendFact fact = new BackendFact(object, subject, verb);
//            facts.add(fact);
//        }

        return facts;
    }

    @Override
    public List<BackendFact> responseToFacts(long backendId) {
        /*
        List<AnswerObject> answers = super.getAnswerObjects();
        List<Response> responses = super.studentResponses;
        List<BackendFact> facts = new ArrayList<>();
        List<QuestionConceptChoice> questionConcepts = questionData.getQuestionConceptChoices();
        QuestionConceptChoice questionConcept = null;
        for (QuestionConceptChoice qcc : questionConcepts) {
            
            if (qcc.getBackend().getId() == backendId) { 
                
                questionConcept = qcc;
            }
        }
        
        for (Response r : responses) {
            
            String object;
            String subject;
            String verb = ;

            if

            BackendFact fact = new BackendFact()
            facts.add()
        }
        */
        return null;
    }

    @Override
    public List<BackendFact> statementToFacts() {
        List<BackendFact> facts = new ArrayList<>();
        return facts;
    }

    @Override
    public Long getExerciseAttemptId() {
        return null;
    }
}
