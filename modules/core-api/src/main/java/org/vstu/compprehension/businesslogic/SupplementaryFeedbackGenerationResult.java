package org.vstu.compprehension.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.frontend.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;

@AllArgsConstructor
public class SupplementaryFeedbackGenerationResult {
    @Getter
    @NotNull final private SupplementaryFeedbackDto feedback;
    @Getter
    @Nullable final private NewSupplementaryStepData newStep;
}
