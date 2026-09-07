package org.vstu.compprehension.data.exerciseattempt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.enums.InteractionType;

import java.util.List;

/**
 * Одно взаимодействие студента с вопросом.
 *
 * @param interactionsLeft null, если у взаимодействия нет feedback. Раньше код писал
 *                         {@code getFeedback().getInteractionsLeft()} и в этом случае
 *                         падал с NPE; поведение сохранено — распаковка null даёт ту же
 *                         ошибку в том же месте.
 * @param violationLawNames имена нарушенных законов
 * @param correctLawNames   имена корректно применённых законов
 */
public record AttemptInteractionData(
        long id,
        int orderNumber,
        @Nullable InteractionType type,
        @Nullable Integer interactionsLeft,
        @NotNull List<String> violationLawNames,
        @NotNull List<String> correctLawNames) {
}
