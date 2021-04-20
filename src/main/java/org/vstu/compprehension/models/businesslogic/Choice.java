package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class Choice extends Question {

    public Choice(QuestionEntity questionData) {
        super(questionData);
    }

    @Override
    public List<BackendFactEntity> responseToFacts(List<ResponseEntity> responses) {

        List<AnswerObjectEntity> answers = new ArrayList<>(super.
                getAnswerObjects());
        List<BackendFactEntity> facts = new ArrayList<>();
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
    public List<BackendFactEntity> responseToFacts(long backendId) {
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
    public Long getExerciseAttemptId() {
        return null;
    }
}
