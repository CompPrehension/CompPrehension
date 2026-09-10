package org.vstu.compprehension.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.question.NewSupplementaryStepData;

@AllArgsConstructor
public class SupplementaryResponseGenerationResult {
    @Getter
    @NotNull private SupplementaryResponse response;
    @Getter
    @Nullable private NewSupplementaryStepData newStep;
}
