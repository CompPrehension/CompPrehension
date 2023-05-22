package org.vstu.compprehension.models.businesslogic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.SupplementaryStepEntity;

@AllArgsConstructor
public class SupplementaryResponseGeneration {
    @Getter
    @NotNull private SupplementaryResponse response;
    @Getter
    @Nullable private SupplementaryStepEntity newStep;
}
