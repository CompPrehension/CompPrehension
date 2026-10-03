package org.vstu.compprehension.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.strategies.settings.StrategySettingsType;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;

public interface AbstractStrategy {

    @NotNull String getStrategyId();

    @NotNull String getDisplayName(Language language);

    @Nullable String getDescription(Language language);

    @NotNull StrategyOptions getOptions();

    /** Настройки, которые преподаватель задаёт стратегии в упражнении. */
    @NotNull StrategySettingsType<?> getSettingsType();

    /** Спросить ли студента о рассуждении, если к его верному ответу ведут и заблуждения. */
    boolean shouldClarifyCorrectAnswer(long exerciseAttemptId);

    QuestionRequest generateQuestionRequest(long exerciseAttemptId);

    /** Оценка попытки после последнего записанного ответа. */
    float grade(long exerciseAttemptId);

    Decision decide(long exerciseAttemptId);
    
    default StrategyDecision gradeAndDecide(long exerciseAttemptId) {
        var grade = grade(exerciseAttemptId);
        var decision = decide(exerciseAttemptId);
        return new StrategyDecision(grade, decision);
    }
}
