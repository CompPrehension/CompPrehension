package org.vstu.compprehension.strategies;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.businesslogic.strategies.NoStrategySettings;
import org.vstu.compprehension.businesslogic.strategies.StrategyOptions;
import org.vstu.compprehension.businesslogic.strategies.StrategyBase;
import org.vstu.compprehension.data.exerciseattempt.AttemptStageData;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.InteractionType;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionInteractionData;
import org.vstu.compprehension.data.exerciseattempt.AttemptQuestionData;

import java.util.List;

@Log4j2
public class StaticStrategy extends StrategyBase {

    private final DomainFactory domainFactory;
    private final StrategyOptions options;

    @Autowired
    public StaticStrategy(DomainFactory domainFactory) {
        super(NoStrategySettings.TYPE);
        this.domainFactory = domainFactory;
        this.options = StrategyOptions.builder()
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
            return "Статическая стратегия задаёт на этапе все запланированные на него вопросы";
        }
        return "Static strategy asks all questions planned for the stage";
    }

    @NotNull
    @Override
    public StrategyOptions getOptions() {
        return options;
    }

    @Override
    public QuestionRequest generateQuestionRequest(@NotNull AttemptStageData stage) {
        Domain domain = domainFactory.getDomain(stage.getAttempt().exercise().domainId());

        QuestionRequest qr = initQuestionRequest(stage, domain);

//        Random random = domain.getRandomProvider().getRandom();
//
//        //  * (0.8 .. 1.2)
//        float changeCoeff = 0.8f + 0.4f * random.nextFloat();
//        float complexity = qr.getComplexity() * changeCoeff;
//        qr.setComplexity(complexity);

        return adjustQuestionRequest(qr, stage);
    }

    @Override
    public float grade(@NotNull AttemptStageData stage) {
        // current progress over questions of the stage
        float cumulativeGrade = 0;
        for(AttemptQuestionData q : stage.getQuestions()) {
            List<AttemptQuestionInteractionData> interactions = q.interactions();
            int knownInteractions = interactions.size();
            long correctInteractions = interactions.stream()
                    .filter(inter -> inter != null
                            && inter.type() == InteractionType.SEND_RESPONSE
                            && inter.isCorrect()
                    )
                    .count();
            if (knownInteractions == 0)
                continue;  // nothing done yet.
            cumulativeGrade += correctInteractions / (float) Math.max(4, knownInteractions);  // don't give best score for just first 1..3 interactions
        }

        return cumulativeGrade / stage.getStage().getNumberOfQuestions();
    }

    @Override
    public Decision decide(@NotNull AttemptStageData stage) {
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
        return Decision.CONTINUE;
    }
}
