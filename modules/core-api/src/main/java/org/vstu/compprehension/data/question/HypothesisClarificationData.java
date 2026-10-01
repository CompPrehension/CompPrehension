package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/** Уточняющий вопрос студенту: каким рассуждением он пришёл к ответу, который объясняют несколько гипотез. */
public record HypothesisClarificationData(@NotNull String prompt, @NotNull List<Option> options) implements Serializable {

    public HypothesisClarificationData {
        options = List.copyOf(options);
    }

    /** Вариант ответа: гипотеза, её формулировка для студента и объяснение этого заблуждения. */
    public record Option(@NotNull String hypothesis, @NotNull String reason, @NotNull String explanation)
            implements Serializable {
    }
}
