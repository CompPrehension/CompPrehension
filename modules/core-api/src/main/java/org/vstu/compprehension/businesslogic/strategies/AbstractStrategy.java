package org.vstu.compprehension.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;

public interface AbstractStrategy {

    @NotNull String getStrategyId();

    @NotNull String getDisplayName(Language language);

    @Nullable String getDescription(Language language);

    @NotNull StrategyOptions getOptions();

    QuestionRequest generateQuestionRequest(long exerciseAttemptId);

    float grade(long exerciseAttemptId, Domain.InterpretSentenceResult judgeResult);

    Decision decide(long exerciseAttemptId);
    
    default StrategyDecision gradeAndDecide(long exerciseAttemptId, Domain.InterpretSentenceResult judgeResult) {
        var grade = grade(exerciseAttemptId, judgeResult);
        var decision = decide(exerciseAttemptId);
        return new StrategyDecision(grade, decision);
    }
}
