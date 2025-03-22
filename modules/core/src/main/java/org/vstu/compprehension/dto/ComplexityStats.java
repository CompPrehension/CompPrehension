package org.vstu.compprehension.dto;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class ComplexityStats {
    Long count;
    @Nullable
    Double min;
    @Nullable
    Double mean;
    @Nullable
    Double max;
}
