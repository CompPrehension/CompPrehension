package org.vstu.compprehension.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.domains.Domain;
import org.vstu.compprehension.data.enums.Decision;
import org.vstu.compprehension.data.enums.Language;

public interface AbstractStrategy {
    @NotNull String getStrategyId();
    @NotNull String getDisplayName(Language language);
    @Nullable String getDescription(Language language);
    @NotNull StrategyOptions getOptions();

    /**
     * Методы принимают идентификаторы, а не сущности: так контракт не приходится менять
     * каждый раз, когда одной из стратегий понадобились новые данные. Загрузка — забота
     * реализации, см. {@link StrategyBase}.
     */
    QuestionRequest generateQuestionRequest(long exerciseAttemptId);

    /**
     * @param exerciseAttemptId attempt to grade
     * @param judgeResult result of judging the student's answer
     * @return grade in range [0..1]
     */
    float grade(long exerciseAttemptId, Domain.InterpretSentenceResult judgeResult);

    Decision decide(long exerciseAttemptId);
}
