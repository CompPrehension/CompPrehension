package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.dto.SupplementaryFeedbackDto;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;

@AllArgsConstructor
public class SupplementaryFeedbackGeneration {
    @Getter
    @NotNull final private SupplementaryFeedbackDto feedback;
    @Getter
    @Nullable final private SupplementaryStepEntity newStep;
}
