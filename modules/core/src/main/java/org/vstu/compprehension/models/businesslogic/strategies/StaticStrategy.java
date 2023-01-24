package org.vstu.compprehension.models.businesslogic.strategies;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.common.MathHelper;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.EnumData.*;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component @Singleton @Primary
@Log4j2
public class StaticStrategy implements AbstractStrategy {

    private final DomainFactory domainFactory;
    private final StrategyOptions options;

    @Autowired
    public StaticStrategy(DomainFactory domainFactory) {
        this.domainFactory = domainFactory;
        this.options = StrategyOptions.builder()
                .multiStagesEnabled(true)
                .visibleToUser(true)
                .build();
    }

    @NotNull
    @Override
    public String getStrategyId() {
        return "StaticStrategy";
    }

    @NotNull
    @Override
    public StrategyOptions getOptions() {
        return options;
    }

    @Override
    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {
        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = domainFactory.getDomain(exercise.getDomain().getName());

        QuestionRequest qr = new QuestionRequest();
        qr.setExerciseAttempt(exerciseAttempt);

        ExerciseStageEntity exerciseStage = getStageForNextQuestion(exerciseAttempt);

        List<ExerciseConceptDto> exConcepts = exerciseStage.getConcepts();
        // take target concepts as is, use their children as `allowed`
        qr.setTargetConcepts (exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.TARGETED)).map(ec -> domain.getConcept(ec.getName())).filter(Objects::nonNull).collect(Collectors.toList()));

        Set<Concept> allowed = exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.PERMITTED)).flatMap(ec -> domain.getConceptWithChildren(ec.getName()).stream()).collect(Collectors.toSet());
        allowed.addAll(exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.TARGETED)).flatMap(ec -> domain.getChildrenOfConcept(ec.getName()).stream()).collect(Collectors.toSet()));
        qr.setAllowedConcepts(List.copyOf(allowed));

        qr.setDeniedConcepts (exConcepts.stream().filter(ec -> ec.getKind().equals(RoleInExercise.FORBIDDEN)).flatMap(ec -> domain.getConceptWithChildren(ec.getName()).stream()).collect(Collectors.toList()));

        List<ExerciseLawDto> exLaws = exerciseStage.getLaws();
        qr.setTargetLaws(exLaws.stream()
                .filter(ec -> ec.getKind()
                        .equals(RoleInExercise.TARGETED))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getName()).stream()))
                .collect(Collectors.toList()));
        qr.setAllowedLaws(exLaws.stream()
                .filter(ec -> ec.getKind()
                        .equals(RoleInExercise.PERMITTED))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getName()).stream()))
                .collect(Collectors.toList()));
        qr.setDeniedLaws(exLaws.stream()
                .filter(ec -> ec.getKind()
                        .equals(RoleInExercise.FORBIDDEN))
                .flatMap(ec -> Stream.concat(
                        domain.getPositiveLawWithImplied(ec.getName()).stream(),
                        domain.getNegativeLawWithImplied(ec.getName()).stream()))
                .collect(Collectors.toList()));

        Concept badConcept = domain.getConcept("SystemIntegrationTest");
        if (badConcept != null)
            qr.getDeniedConcepts().add(badConcept);
//        HashMap<String, List<Boolean>> allLaws = getTargetLawsInteractions(exerciseAttempt, 0);
//        HashMap<String, List<Boolean>> allLawsBeforeLastQuestion = getTargetLawsInteractions(exerciseAttempt, 1);

        qr.setDeniedQuestionNames(listQuestionNamesOfAttempt(exerciseAttempt));
        qr.setDeniedQuestionTemplateIds(listQuestionsOfAttempt(exerciseAttempt).stream().map(q -> q.getOptions().getTemplateId()).filter(id -> id != -1).collect(Collectors.toList()));

//        qr.setComplexitySearchDirection(SearchDirections.TO_SIMPLE);
        qr.setComplexitySearchDirection(null);
         qr.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
        qr.setChanceToPickAutogeneratedQuestion(1.0);
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
        // all questions defined by exercise
        int nQuestionsExpected = exerciseAttempt.getExercise().getStages().stream().mapToInt(ExerciseStageEntity::getNumberOfQuestions).reduce(Integer::sum).orElse(1);
        // current progress over all questions
        float cumulativeGrade = 0;
        for(QuestionEntity q : exerciseAttempt.getQuestions()) {
            List<InteractionEntity> interactions = q.getInteractions();
            int knownInteractions = interactions.size();
            long correctInteractions = interactions.stream()
                    .filter(inter -> inter != null && (inter.getViolations() == null || inter.getViolations().isEmpty()))
                    .count();
            if (knownInteractions == 0)
                continue;  // nothing done yet.
            cumulativeGrade += correctInteractions / (float) Math.max(4, knownInteractions);  // don't give best score for just first 1..3 interactions
        }

        return cumulativeGrade / nQuestionsExpected;
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        List<QuestionEntity> questions = exerciseAttempt.getQuestions();

        // get limit of questions defined by teacher in exercise GUI
        int minimumQuestionsToAsk = getNumberOfQuestionsToAsk(exerciseAttempt.getExercise());

        // Должно быть задано не менее X вопросов и последний вопрос должен быть завершён (завершение упражнения возможно только в момент завершения вопроса)
        if(questions.size() < minimumQuestionsToAsk ||
                questions.stream().anyMatch(q -> (long)(q.getId()) == questions.get(questions.size() - 1).getId() && (q.getInteractions().size() == 0 || q.getInteractions().get(q.getInteractions().size() - 1).getFeedback().getInteractionsLeft() > 0))){
            return Decision.CONTINUE;
        }

        // Должно быть задано не менее X вопросов, которые были завершены
        long completedQuestions = questions.stream()
                .filter(q -> q.getInteractions().size() > 0 && q.getInteractions().get(q.getInteractions().size() - 1).getFeedback().getInteractionsLeft() == 0)
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
