package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.businesslogic.backend.facts.Fact;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Choice extends Question {

    public Choice(QuestionEntity questionData, Domain domain) {
        super(questionData, domain);
    }

    @Override
    public Collection<Fact> responseToFacts(List<ResponseEntity> responses) {

        List<AnswerObjectEntity> answers = new ArrayList<>(super.
                getAnswerObjects());
        List<Fact> facts = new ArrayList<>();
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

    public Long getExerciseAttemptId() {
        return null;
    }
}
