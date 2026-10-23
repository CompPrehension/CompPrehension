package org.vstu.compprehension.businesslogic;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.SupplementaryStepData;

/**
 * Шаг цепочки наводящих вопросов вместе с взаимодействием с основным вопросом, для которого эта цепочка была создана.
 */
public record SupplementaryStepContext(
        @NotNull SupplementaryStepData step,
        @NotNull QuestionInteractionData mainQuestionInteraction
) {
}
