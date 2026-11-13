package org.vstu.compprehension.businesslogic.reaction;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.settings.Setting;
import org.vstu.compprehension.businesslogic.settings.SettingsType;

/** Как тренажёр в упражнении реагирует на ответы студента, которые объясняют рассуждения. */
public record AnswerReactionSettings(
        @Setting(ru = "Какие рассуждения допускать", en = "Reasonings to admit")
        @NotNull ReasoningSelection reasoningSelection,
        @Setting(ru = "Уточнять рассуждение при неверном ответе", en = "Ask about reasoning behind wrong answers")
        @NotNull WrongAnswerClarification wrongAnswerClarification,
        @Setting(ru = "Уточнять рассуждение при верном ответе", en = "Ask about reasoning behind correct answers")
        @NotNull CorrectAnswerClarification correctAnswerClarification) {

    public static final SettingsType<AnswerReactionSettings> TYPE = new SettingsType<>(
            AnswerReactionSettings.class,
            new AnswerReactionSettings(ReasoningSelection.FEWEST_ERRORS, WrongAnswerClarification.WHEN_AMBIGUOUS,
                    new CorrectAnswerClarification(CorrectAnswerClarification.Mode.NEVER, 7)));
}
