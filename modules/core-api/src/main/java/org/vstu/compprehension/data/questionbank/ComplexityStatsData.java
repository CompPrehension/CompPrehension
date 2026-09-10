package org.vstu.compprehension.data.questionbank;

import org.jetbrains.annotations.Nullable;

public record ComplexityStatsData(long count, @Nullable Double min, @Nullable Double mean, @Nullable Double max) {
}
