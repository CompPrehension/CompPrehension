package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.SupplementaryStepData;

public interface SupplementaryStepDataService {
    @Nullable SupplementaryStepData findLatestStepOfInteraction(long interactionId);

    @NotNull QuestionInteractionData getMainQuestionInteraction(long interactionId);
}
