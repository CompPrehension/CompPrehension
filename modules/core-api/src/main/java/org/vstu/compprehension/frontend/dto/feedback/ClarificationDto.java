package org.vstu.compprehension.frontend.dto.feedback;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Уточняющий вопрос о рассуждении студента; объяснения вариантов студенту не отправляются до выбора. */
public record ClarificationDto(@NotNull String prompt, @NotNull List<Option> options) {

    /** @param id номер рассуждения, о котором вариант: у разных вариантов гипотеза может совпадать */
    public record Option(int id, @NotNull String hypothesis, @NotNull String reason) {
    }
}
