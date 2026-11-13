package org.vstu.compprehension.businesslogic.strategies;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.businesslogic.settings.SettingsType;
import org.vstu.compprehension.data.exerciseattempt.AttemptStageData;
import org.vstu.compprehension.enums.Decision;
import org.vstu.compprehension.enums.Language;

public interface AbstractStrategy {

    @NotNull String getStrategyId();

    @NotNull String getDisplayName(Language language);

    @Nullable String getDescription(Language language);

    @NotNull StrategyOptions getOptions();

    /** Настройки, которые преподаватель задаёт стратегии в этапе упражнения. */
    @NotNull SettingsType<?> getSettingsType();

    QuestionRequest generateQuestionRequest(@NotNull AttemptStageData stage);

    /** Оценка этапа после последнего записанного ответа: доля от вопросов, запланированных на этап. */
    float grade(@NotNull AttemptStageData stage);

    /** {@link Decision#FINISH}, если этап пройден раньше, чем заданы все запланированные на него вопросы. */
    Decision decide(@NotNull AttemptStageData stage);
}
