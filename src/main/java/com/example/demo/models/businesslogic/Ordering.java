package com.example.demo.models.businesslogic;

import com.example.demo.models.businesslogic.backend.BackendFact;
import com.example.demo.models.businesslogic.frontend.FrontAnswerElement;
import com.example.demo.models.entities.*;
import com.example.demo.utils.HyperText;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Ordering extends Question {

    public Ordering(com.example.demo.models.entities.Question questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFact> responseToFacts() {

        List<AnswerObject> answers = new ArrayList<>(super.
                getAnswerObjects());
        List<Response> responses = super.studentResponses;
        List<BackendFact> facts = new ArrayList<>();
        QuestionConceptOrder questionConcept = questionData.
                getQuestionConceptOrders().get(0);
        //Формируем факты из ответов студент
        for (Response r : responses) {
            //Формируем элементы триплета
            String object = (r.getSpecValue() == null) ? r.getLeftAnswerObject().
                    getConcept(): questionConcept.getStartConcept();
            String subject = r.getRightAnswerObject().getConcept();
            String verb = questionConcept.getFollowVerb();
            //Удаляем элемент ответа, т.к. на основе него уже сформирован факт
            answers.remove(r.getRightAnswerObject());
            answers.remove(r.getLeftAnswerObject());
            //Создаем на основе триплета факт           
            BackendFact fact = new BackendFact(object, subject, verb);
            facts.add(fact);
        }
        //Формируем факты из невыбранных ответов
        for (AnswerObject ao : answers) {
            //Формируем элементы триплета
            String object = questionConcept.getNotInOrderConcept();
            String subject = ao.getConcept();
            String verb = questionConcept.getNotInOrderVerb();
            //Создаем на основе триплета факт
            BackendFact fact = new BackendFact(object, subject, verb);
            facts.add(fact);
        }

        return facts;        
    }

    @Override
    public List<BackendFact> responseToFacts(long backendId) {
        return null;
    }
}
