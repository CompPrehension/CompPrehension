package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.SupplementaryStepData;

public interface SupplementaryStepDataService {
    @Nullable SupplementaryStepData findLatestStepOfInteraction(long interactionId);

    @NotNull QuestionData getMainQuestionOfInteraction(long interactionId);
}
