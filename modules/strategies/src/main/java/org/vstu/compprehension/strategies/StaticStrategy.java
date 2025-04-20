package org.vstu.compprehension.strategies;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.models.businesslogic.Concept;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.businesslogic.strategies.StrategyOptions;
import org.vstu.compprehension.models.entities.EnumData.*;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;

import java.util.List;

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
    public String getDisplayName(Language language) {
        if (language == Language.RUSSIAN) {
            return "Статическая стратегия";
        }
        return "Static strategy";
    }

    @Nullable
    @Override
    public String getDescription(Language language) {
        if (language == Language.RUSSIAN) {
            return "Статическая стратегия состоит из нескольких этапов с фиксированным количеством вопросов в каждом";
        }
        return "Static strategy consists of several stages with a fixed number of questions in each stage";
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

        ExerciseStageEntity exerciseStage = getStageForNextQuestion(exerciseAttempt);

        QuestionRequest qr = initQuestionRequest(exerciseAttempt, exerciseStage, domain);

        Concept badConcept = domain.getConcept("SystemIntegrationTest");
        if (badConcept != null)
            qr.getDeniedConcepts().add(badConcept);

//        Random random = domain.getRandomProvider().getRandom();
//
//        //  * (0.8 .. 1.2)
//        float changeCoeff = 0.8f + 0.4f * random.nextFloat();
//        float complexity = qr.getComplexity() * changeCoeff;
//        qr.setComplexity(complexity);

        return adjustQuestionRequest(qr, exerciseAttempt);
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
                    .filter(inter -> inter != null
                            && inter.getInteractionType() == InteractionType.SEND_RESPONSE
                            && (inter.getViolations() == null || inter.getViolations().isEmpty())
                    )
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
