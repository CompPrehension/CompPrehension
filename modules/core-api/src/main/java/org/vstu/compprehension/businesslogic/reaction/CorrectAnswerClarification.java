package org.vstu.compprehension.businesslogic.reaction;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.settings.Setting;

/** Когда спрашивать о рассуждении, если к верному ответу ведут и заблуждения. */
public record CorrectAnswerClarification(
        @Setting(ru = "Режим", en = "Mode")
        @NotNull Mode mode,
        @Setting(ru = "Длина серии верных ответов", en = "Correct answer streak length", min = 1)
        int streakLength) {

    public enum Mode {
        @Setting(ru = "Никогда", en = "Never")
        NEVER,
        @Setting(ru = "Всегда", en = "Always")
        ALWAYS,
        @Setting(ru = "До серии верных ответов", en = "Until a streak of correct answers")
        UNTIL_STREAK
    }
}
