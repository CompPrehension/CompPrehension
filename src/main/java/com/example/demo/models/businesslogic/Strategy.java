package com.example.demo.models.businesslogic;

import com.example.demo.Service.DomainService;
import com.example.demo.Service.QuestionAttemptService;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.entities.EnumData.DisplayingFeedbackType;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.utils.DomainAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Strategy extends AbstractStrategy {
    
    @Autowired
    private QuestionAttemptService questionAttemptService;

    @Autowired
    private DomainService domainService;

    public QuestionRequest generateQuestionRequest(ExerciseAttempt exerciseAttempt) {

        QuestionRequest qr = new QuestionRequest();
        Exercise exercise = exerciseAttempt.getExercise();
        Domain domain = DomainAdapter.getDomain(domainService.getDomainEntity(exercise.getDomain().getName()).getName());

        List<Concept> deniedConcepts = new ArrayList<>();
        List<Concept> allowedConcepts = new ArrayList<>();
        List<Concept> targetConcepts = new ArrayList<>();

        //Выделить из упражнения целевые и запрещенные законы
        for (ExerciseConcept ec : exercise.getExerciseConcepts()) {
            if (ec.getRoleInExercise() == RoleInExercise.TARGETED) {
                targetConcepts.add(domain.getConcept(ec.getConceptName()));
            } else if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {
                deniedConcepts.add(domain.getConcept(ec.getConceptName()));
            }
        }

        if(targetConcepts.contains(domain.getConcept("precedence"))){
            allowedConcepts.add(domain.getConcept("operator_binary_*"));
            allowedConcepts.add(domain.getConcept("operator_binary_+"));
        }else{
            allowedConcepts.add(domain.getConcept("operator_binary_+"));
        }

        qr.setComplexity(1);
        qr.setDeniedConcepts(deniedConcepts);
        qr.setSolvingDuration(30);
        qr.setTargetConcepts(targetConcepts);
        qr.setAllowedConcepts(allowedConcepts);

        List<Law> laws = new ArrayList<>(domain.getNegativeLaws()); //domainEntity.getLaws();

        List<Law> targetLaws = new ArrayList<>();
        List<Law> deniedLaws = new ArrayList<>();
        //Распределяем законы не запрещенные и целевые
        for (Law l : laws) {
            boolean isLawContainsDeniedConcepts = false;
            boolean isLawContainsTargetConcepts = false;
            //Проверить, содержит ли закон запрещенные концепты
            for (int i = 0; i < deniedConcepts.size() && isLawContainsDeniedConcepts == false; ++i) {
                isLawContainsDeniedConcepts = l.getConcepts().contains(
                        deniedConcepts.get(i));
            }

            //Проверить, содержит ли закон целевые концепты
            for (int i = 0; i < targetConcepts.size() && isLawContainsTargetConcepts == false; ++i) {
                isLawContainsTargetConcepts = l.getConcepts().contains(
                        targetConcepts.get(i));
            }
            if (isLawContainsDeniedConcepts) {
                deniedLaws.add(l);
            } else if (isLawContainsTargetConcepts) {
                targetLaws.add(l);
            }
        }

        qr.setDeniedLaws(deniedLaws);
        qr.setTargetLaws(targetLaws);

        return qr;
    }

    public DisplayingFeedbackType determineDisplayingFeedbackType(QuestionAttempt questionAttempt) {

        List<Interaction> interactions = questionAttempt.getInteractions();

        int interactionWithMistakes = 0;
        for (Interaction i : interactions) {

            if (i.getMistakes() != null || i.getMistakes().size() != 0) {

                interactionWithMistakes++;
            }
        }

        if (interactionWithMistakes < 1) {
            return DisplayingFeedbackType.NOT_SHOW;
        } else if (interactionWithMistakes < 2) {
            return DisplayingFeedbackType.HOVER;
        } else {
            return DisplayingFeedbackType.SHOW;
        }
    }

    public FeedbackType determineFeedbackType(QuestionAttempt questionAttempt) {

        List<Interaction> interactions = questionAttempt.getInteractions();

        int interactionWithMistakes = 0;
        for (Interaction i : interactions) {

            if (i.getMistakes() != null || i.getMistakes().size() != 0) {

                interactionWithMistakes++;
            }
        }

        if (interactionWithMistakes < 1) {
            return FeedbackType.DEGREE_OF_CORRECTNESS;
        } else {
            return FeedbackType.EXPLANATION;
        }
    }
}
