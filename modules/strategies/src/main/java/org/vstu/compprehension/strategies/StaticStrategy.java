package org.vstu.compprehension.strategies;

import org.vstu.compprehension.data.exercise.ExerciseStageData;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.strategies.StrategyOptions;
import org.vstu.compprehension.businesslogic.strategies.StrategyBase;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.services.ExerciseAttemptDataService;
import org.vstu.compprehension.data.exerciseattempt.AttemptExerciseData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;

import java.util.List;

@Log4j2
public class StaticStrategy extends StrategyBase {

    private final DomainFactory domainFactory;
    private final StrategyOptions options;

    @Autowired
    public StaticStrategy(DomainFactory domainFactory, ExerciseAttemptDataService exerciseAttemptService) {
        super(exerciseAttemptService);
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
    public QuestionRequest generateQuestionRequest(long exerciseAttemptId) {
        var exerciseAttempt = getAttempt(exerciseAttemptId);
        AttemptExerciseData exercise = exerciseAttempt.exercise();
        Domain domain = domainFactory.getDomain(exercise.domainName());

        ExerciseStageData exerciseStage = getStageForNextQuestion(exerciseAttempt);

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
    public float grade(long exerciseAttemptId, Domain.InterpretSentenceResult judgeResult) {
        var exerciseAttempt = getAttempt(exerciseAttemptId);
        // all questions defined by exercise
        int nQuestionsExpected = exerciseAttempt.exercise().stages().stream().mapToInt(ExerciseStageData::getNumberOfQuestions).reduce(Integer::sum).orElse(1);
        // current progress over all questions
        float cumulativeGrade = 0;
        for(AttemptQuestionData q : exerciseAttempt.questions()) {
            List<AttemptQuestionInteractionData> interactions = q.interactions();
            int knownInteractions = interactions.size();
            long correctInteractions = interactions.stream()
                    .filter(inter -> inter != null
                            && inter.type() == InteractionType.SEND_RESPONSE
                            && (inter.violationLawNames() == null || inter.violationLawNames().isEmpty())
                    )
                    .count();
            if (knownInteractions == 0)
                continue;  // nothing done yet.
            cumulativeGrade += correctInteractions / (float) Math.max(4, knownInteractions);  // don't give best score for just first 1..3 interactions
        }

        return cumulativeGrade / nQuestionsExpected;
    }

    @Override
    public Decision decide(long exerciseAttemptId) {
        var exerciseAttempt = getAttempt(exerciseAttemptId);
        List<AttemptQuestionData> questions = exerciseAttempt.questions();

        // get limit of questions defined by teacher in exercise GUI
        int minimumQuestionsToAsk = getNumberOfQuestionsToAsk(exerciseAttempt.exercise());

        // Должно быть задано не менее X вопросов и последний вопрос должен быть завершён (завершение упражнения возможно только в момент завершения вопроса)
        if(questions.size() < minimumQuestionsToAsk ||
                questions.stream().anyMatch(q -> q.questionId() == questions.get(questions.size() - 1).questionId() && (q.interactions().size() == 0 || q.interactions().get(q.interactions().size() - 1).interactionsLeft() > 0))){
            return Decision.CONTINUE;
        }

        // Должно быть задано не менее X вопросов, которые были завершены
        long completedQuestions = questions.stream()
                .filter(q -> q.interactions().size() > 0 && q.interactions().get(q.interactions().size() - 1).interactionsLeft() == 0)
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
