package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Итог записи взаимодействия: всё, что об этом вопросе нужно знать для ответа фронту.
 * <p>
 * Счётчики и последнее верное взаимодействие считаются здесь же, а не вызывающим,
 * потому что считаются они по всем взаимодействиям вопроса — включая только что
 * записанное, которого у вызывающего ещё нет.
 *
 * @param latestCorrectInteraction последнее взаимодействие без нарушений и с
 *                                 неисчерпанным остатком попыток; null, если такого нет
 */
public record RecordedInteractionData(
        long interactionId,
        int correctInteractionsCount,
        int erroneousInteractionsCount,
        @NotNull List<ResponseData> responses,
        @Nullable InteractionResponsesData latestCorrectInteraction) {
}
