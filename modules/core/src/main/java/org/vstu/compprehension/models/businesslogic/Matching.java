package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;

import java.util.Collection;
import java.util.List;

public class Matching extends Question {

    public Matching(QuestionEntity questionData, Domain domain) {
        super(questionData, domain);
    }

    public Collection<Fact> responseToFacts(List<ResponseEntity> responses) {
        return domain.responseToFacts(
                getQuestionDomainType(),
                responses,
                getAnswerObjects()
        );

        //List<AnswerObjectEntity> answers = new ArrayList<>(super.
        //        getAnswerObjects());
        //List<ResponseEntity> responses = super.studentResponses;
        //List<BackendFactEntity> facts = new ArrayList<>();
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

        //return facts;
        
    }

    public Long getExerciseAttemptId() {
        return null;
    }
}
