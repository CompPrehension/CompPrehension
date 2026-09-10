package org.vstu.compprehension.data.question;

import org.jetbrains.annotations.Nullable;

/**
 * Ответ в составе взаимодействия: либо уже сохранённый, либо тот, что предстоит завести.
 */
public record NewInteractionAnswerData(
        @Nullable Long responseId,
        int leftAnswerId,
        int rightAnswerId,
        @Nullable Long createdByInteractionId) {
}
