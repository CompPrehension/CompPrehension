package org.vstu.compprehension.businesslogic.strategies.settings;

import org.jetbrains.annotations.NotNull;

/** Настройки стратегии без собственных параметров. */
public record CommonStrategySettings(
        @Setting(ru = "Уточнять рассуждение при верном ответе", en = "Ask about reasoning behind correct answers")
        @NotNull CorrectAnswerClarification correctAnswerClarification) implements StrategySettings {

    public static final StrategySettingsType<CommonStrategySettings> TYPE = new StrategySettingsType<>(
            CommonStrategySettings.class,
            new CommonStrategySettings(new CorrectAnswerClarification(CorrectAnswerClarification.Mode.NEVER, 7)));
}
