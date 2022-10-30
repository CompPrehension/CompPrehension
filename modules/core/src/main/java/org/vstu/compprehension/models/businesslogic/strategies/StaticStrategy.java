package org.vstu.compprehension.models.businesslogic.strategies;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.*;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component @Singleton @Primary
@Log4j2
public class StaticStrategy implements AbstractStrategy {

    private DomainFactory domainFactory;

    @Autowired
    public StaticStrategy(DomainFactory domainFactory) {
        this.domainFactory = domainFactory;
    }

    @NotNull
    @Override
    public String getStrategyId() {
        return "StaticStrategy";
    }

    @Override
    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {
        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = domainFactory.getDomain(exercise.getDomain().getName());

        QuestionRequest qr = new QuestionRequest();

        List<ExerciseConceptEntity> exConcepts = exercise.getExerciseConcepts();
        qr.setTargetConcepts (exConcepts.stream().filter(ec -> ec.getRoleInExercise().equals(RoleInExercise.TARGETED)).map(ec -> domain.getConcept(ec.getConceptName())).collect(Collectors.toList()));
        qr.setAllowedConcepts(exConcepts.stream().filter(ec -> ec.getRoleInExercise().equals(RoleInExercise.PERMITTED)).map(ec -> domain.getConcept(ec.getConceptName())).collect(Collectors.toList()));
        qr.setDeniedConcepts (exConcepts.stream().filter(ec -> ec.getRoleInExercise().equals(RoleInExercise.FORBIDDEN)).map(ec -> domain.getConcept(ec.getConceptName())).collect(Collectors.toList()));

        List<ExerciseLawsEntity> exLaws = exercise.getExerciseLaws();
        qr.setTargetLaws(exLaws.stream()
                .filter(ec -> ec.getRoleInExercise()
                        .equals(RoleInExercise.TARGETED))
                .map(ec -> Optional.ofNullable((Law)domain.getPositiveLaw(ec.getLawName()))
                        .orElse(domain.getNegativeLaw(ec.getLawName())))
                .collect(Collectors.toList()));
        qr.setAllowedLaws(exLaws.stream()
                .filter(ec -> ec.getRoleInExercise()
                        .equals(RoleInExercise.PERMITTED))
                .map(ec -> Optional.ofNullable((Law)domain.getPositiveLaw(ec.getLawName()))
                        .orElse(domain.getNegativeLaw(ec.getLawName())))
                .collect(Collectors.toList()));
        qr.setDeniedLaws(exLaws.stream()
                .filter(ec -> ec.getRoleInExercise()
                        .equals(RoleInExercise.FORBIDDEN))
                .map(ec -> Optional.ofNullable((Law)domain.getPositiveLaw(ec.getLawName()))
                        .orElse(domain.getNegativeLaw(ec.getLawName())))
                .collect(Collectors.toList()));
//        HashMap<String, List<Boolean>> allLaws = getTargetLawsInteractions(exerciseAttempt, 0);
//        HashMap<String, List<Boolean>> allLawsBeforeLastQuestion = getTargetLawsInteractions(exerciseAttempt, 1);

        qr.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
        qr.setChanceToPickAutogeneratedQuestion(0.95);
        qr.setSolvingDuration(10 * exercise.getTimeLimit());  // random duration from [1..10] range
        qr.setComplexity(0.02f * exercise.getComplexity());  // [0..1] -> but [0..0.3]
        return qr;
    }

    @Override
    public DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question) {
        return null;
    }

    @Override
    public FeedbackType determineFeedbackType(QuestionEntity question) {
        return null;
    }

    @Override
    public float grade(ExerciseAttemptEntity exerciseAttempt) {
        // consider last question only ...
        QuestionEntity lastQuestion = exerciseAttempt.getQuestions().stream().reduce((e1, e2) -> e2).orElse(null);
        if (lastQuestion == null)
            return 0;

        List<InteractionEntity> interactions = lastQuestion.getInteractions();
        int knownInteractions = interactions.size();
        if (knownInteractions == 0)
            return 1;  // no mistakes yet. :)

        int correctInteractions = (int) interactions.stream()
                .map(inter -> inter != null && (inter.getViolations() == null || inter.getViolations().isEmpty()))
                .count();

        return (float) correctInteractions / knownInteractions;
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        // TODO: find / get limit of questions defined by teacher in exercise GUI
        int minimumQuestionsToAsk = exerciseAttempt.getExercise().getNumberOfQuestions();
        List<QuestionEntity> questions = exerciseAttempt.getQuestions();
        boolean enoughQuestions = questions.size() >= minimumQuestionsToAsk;
        if (!enoughQuestions)
            return Decision.CONTINUE;

        /*Integer timeLimit = exerciseAttempt.getExercise().getTimeLimit(); // assuming minutes - NO! steps.

        if (timeLimit == null) {
            return Decision.CONTINUE;
        } else {
            if (timeLimit <= 0 || questions.isEmpty())
                return Decision.CONTINUE;
            else if (!questions.isEmpty()) {
                List<InteractionEntity> interactions = questions.get(0).getInteractions();
                if (interactions == null || interactions.isEmpty())
                    return Decision.CONTINUE;

                Instant beginExerciseTime = interactions.get(0).getDate().toInstant();
                Instant now = new Date().toInstant();
                boolean tooLate = beginExerciseTime.plusSeconds(timeLimit * 60).compareTo(now) < 0;
                if (!tooLate)
                    return Decision.CONTINUE;
            }
        }*/
        return Decision.FINISH;
    }
}
