package org.vstu.compprehension.businesslogic.backend;

import its.reasoner.nodes.DecisionTreeTrace;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.domains.Domain;

/**
 * Результат интерпретации ответа, полученный рассуждением по дереву решений.
 */
public class DecisionTreeInterpretSentenceResult extends Domain.InterpretSentenceResult {

    /**
     * The trace of reasoning along the decision tree
     */
    @Nullable
    public DecisionTreeTrace decisionTreeTrace = null;
}
