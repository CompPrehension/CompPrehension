package org.vstu.compprehension.businesslogic.strategies.settings;

import org.jetbrains.annotations.NotNull;

/** Настройки стратегии, которые преподаватель задаёт в упражнении. */
public interface StrategySettings {

    @NotNull ReasoningSelection reasoningSelection();

    @NotNull WrongAnswerClarification wrongAnswerClarification();

    @NotNull CorrectAnswerClarification correctAnswerClarification();
}
