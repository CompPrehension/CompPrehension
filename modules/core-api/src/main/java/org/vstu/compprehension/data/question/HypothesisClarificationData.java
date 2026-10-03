package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/** Уточняющий вопрос студенту: каким рассуждением он пришёл к ответу, который объясняют несколько гипотез. */
public record HypothesisClarificationData(@NotNull String prompt, @NotNull List<Option> options) implements Serializable {

    public HypothesisClarificationData {
        options = List.copyOf(options);
    }

    /**
     * Вариант ответа: рассуждение взаимодействия, гипотезу и причину которого видит студент.
     *
     * @param reasoning   номер рассуждения взаимодействия
     * @param explanation объяснение, которое студент увидит, выбрав вариант
     */
    public record Option(int reasoning, @NotNull String explanation) implements Serializable {
    }
}
