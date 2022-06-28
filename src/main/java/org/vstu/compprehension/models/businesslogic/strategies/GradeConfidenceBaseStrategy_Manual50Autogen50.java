package org.vstu.compprehension.models.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;

import org.springframework.stereotype.Component;
import org.vstu.compprehension.utils.RandomProvider;

import javax.inject.Singleton;

/**
 * Modification of GradeConfidenceBaseStrategy that sets 50/50 probability to select manual or auto-generated question.
 * This strategy is intended for surveys when a student is to distinguish between manual and autogenerated questions.
 */
@Component @Singleton
public class GradeConfidenceBaseStrategy_Manual50Autogen50 extends GradeConfidenceBaseStrategy {
    private final RandomProvider randomProvider;

    @Autowired
    public GradeConfidenceBaseStrategy_Manual50Autogen50(DomainFactory domainFactory, RandomProvider randomProvider) {
        super(domainFactory);
        this.randomProvider = randomProvider;
    }

    @NotNull
    @Override
    public String getStrategyId() {
        return this.getClass().getSimpleName();  // GradeConfidenceBaseStrategy_Manual50Autogen50
    }

    @Override
    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {
        QuestionRequest qr = super.generateQuestionRequest(exerciseAttempt);
        qr.setLawsSearchDirection(SearchDirections.TO_SIMPLE);
        qr.setChanceToPickAutogeneratedQuestion(0.55);
        qr.setSolvingDuration(5 + randomProvider.getRandom().nextInt(6));  // random duration from [1..10] range
//        qr.setComplexity(qr.getComplexity() * 2);
        qr.setComplexity(0.00f + randomProvider.getRandom().nextFloat() * 0.02f);  // [0..1]
        return qr;
    }
}
