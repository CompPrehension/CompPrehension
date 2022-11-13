package org.vstu.compprehension.models.businesslogic.strategies;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.EnumData.*;
import org.vstu.compprehension.models.entities.*;

import javax.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        qr.setExerciseAttempt(exerciseAttempt);

        List<ExerciseConceptEntity> exConcepts = exercise.getExerciseConcepts();
        qr.setTargetConcepts (exConcepts.stream().filter(ec -> ec.getRoleInExercise().equals(RoleInExercise.TARGETED)).flatMap(ec -> domain.getConceptWithChildren(ec.getConceptName()).stream()).collect(Collectors.toList()));
        qr.setAllowedConcepts(exConcepts.stream().filter(ec -> ec.getRoleInExercise().equals(RoleInExercise.PERMITTED)).flatMap(ec -> domain.getConceptWithChildren(ec.getConceptName()).stream()).collect(Collectors.toList()));
        qr.setDeniedConcepts (exConcepts.stream().filter(ec -> ec.getRoleInExercise().equals(RoleInExercise.FORBIDDEN)).flatMap(ec -> domain.getConceptWithChildren(ec.getConceptName()).stream()).collect(Collectors.toList()));

        List<ExerciseLawsEntity> exLaws = exercise.getExerciseLaws();
        qr.setTargetLaws(exLaws.stream()
                .filter(ec -> ec.getRoleInExercise()
                        .equals(RoleInExercise.TARGETED))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getLawName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getLawName()).stream()))
                .collect(Collectors.toList()));
        qr.setAllowedLaws(exLaws.stream()
                .filter(ec -> ec.getRoleInExercise()
                        .equals(RoleInExercise.PERMITTED))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getLawName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getLawName()).stream()))
                .collect(Collectors.toList()));
        qr.setDeniedLaws(exLaws.stream()
                .filter(ec -> ec.getRoleInExercise()
                        .equals(RoleInExercise.FORBIDDEN))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getLawName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getLawName()).stream()))
                .collect(Collectors.toList()));
//        HashMap<String, List<Boolean>> allLaws = getTargetLawsInteractions(exerciseAttempt, 0);
//        HashMap<String, List<Boolean>> allLawsBeforeLastQuestion = getTargetLawsInteractions(exerciseAttempt, 1);

        qr.setDeniedQuestionNames(listQuestionsOfAttempt(exerciseAttempt));
//        qr.setComplexitySearchDirection(SearchDirections.TO_SIMPLE);
        qr.setComplexitySearchDirection(null);
         qr.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
        qr.setChanceToPickAutogeneratedQuestion(0.95);
        qr.setSolvingDuration(exercise.getTimeLimit());  // normalized duration in [1..10] range
        qr.setComplexity(exercise.getComplexity());  // [0..1], copy as is
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
        long maxQid = exerciseAttempt.getQuestions().stream().max(Comparator.comparing(QuestionEntity::getId)).get().getId();
        QuestionEntity lastQuestion = exerciseAttempt.getQuestions().stream().filter(q -> q.getId() == maxQid).findAny().orElse(null);
        if (lastQuestion == null)
            return 0;

        List<InteractionEntity> interactions = lastQuestion.getInteractions();
        int knownInteractions = interactions.size();
        if (knownInteractions == 0)
            return 1;  // no mistakes yet. :)

        long correctInteractions = interactions.stream()
                .filter(inter -> inter != null && (inter.getViolations() == null || inter.getViolations().isEmpty()))
                .count();

        return (float) correctInteractions / knownInteractions;
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        List<QuestionEntity> questions = exerciseAttempt.getQuestions();

        // get limit of questions defined by teacher in exercise GUI
        int minimumQuestionsToAsk = exerciseAttempt.getExercise().getNumberOfQuestions();

        // Должно быть задано не менее X вопросов и последний вопрос должен быть завершён (завершение упражнения возможно только в момент завершения вопроса)
        if(questions.size() < minimumQuestionsToAsk ||
                questions.stream().anyMatch(q -> (long)(q.getId()) == questions.get(questions.size() - 1).getId() && (q.getInteractions().size() == 0 || q.getInteractions().get(q.getInteractions().size() - 1).getFeedback().getInteractionsLeft() > 0))){
            return Decision.CONTINUE;
        }

        // Должно быть задано не менее X вопросов, которые были завершены
        long completedQuestions = questions.stream()
                .filter(q -> q.getInteractions().size() > 0 || q.getInteractions().get(q.getInteractions().size() - 1).getFeedback().getInteractionsLeft() == 0)
                .count();
        if(completedQuestions < minimumQuestionsToAsk)
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
