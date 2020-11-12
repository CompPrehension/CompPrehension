package com.example.demo.models.businesslogic;

import com.example.demo.models.businesslogic.backend.BackendFact;
import com.example.demo.models.businesslogic.frontend.FrontAnswerElement;
import com.example.demo.models.entities.*;
import com.example.demo.utils.HyperText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Matching extends Question {

    public Matching(com.example.demo.models.entities.Question questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFact> responseToFacts() {

        List<AnswerObject> answers = new ArrayList<>(super.
                getAnswerObjects());
        List<Response> responses = super.studentResponses;
        List<BackendFact> facts = new ArrayList<>();
//        QuestionConceptMatch questionConcept = questionData.
//                getQuestionConceptMatches().get(0);
//        //Формируем факты из ответов студент
//        for (Response r : responses) {
//            //Формируем элементы триплета
//            String object = r.getLeftAnswerObject().getConcept();
//            String subject = r.getRightAnswerObject().getConcept();
//            String verb = questionConcept.getMatchVerb();
//            //Удаляем вариант ответа и элемент левого столбца, с которым
//            // он был соотнесен, т.к. на основе него уже сформирован факт
//            answers.remove(r.getLeftAnswerObject());
//            answers.remove(r.getRightAnswerObject());
//            //Создаем на основе триплета факт
//            BackendFact fact = new BackendFact(object, subject, verb);
//            facts.add(fact);
//        }
//
//        //Формируем факты из невыбранных ответов
//        for (AnswerObject ao : answers) {
//
//            String object = "";
//            String subject = "";
//            String verb =  "";
//
//            if (ao.isRightCol()) {  //Вариант ответа остался невыбранным
//
//                object = questionConcept.getNoMatchLeftConcept();
//                subject = ao.getConcept();
//                verb = questionConcept.getNoMatchLeftVerb();
//            } else {    //К этому элементу левой колонки не поставили в пару
//                        //вариант ответа
//                object = ao.getConcept();
//                subject = questionConcept.getNoMatchRightConcept();
//                verb = questionConcept.getNoMatchRightVerb();
//            }
//
//            //Создаем на основе триплета факт
//            BackendFact fact = new BackendFact(object, subject, verb);
//            facts.add(fact);
//        }

        return facts;
        
    }

    @Override
    public List<BackendFact> responseToFacts(long backendId) {
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
