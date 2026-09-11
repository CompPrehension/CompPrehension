package org.vstu.compprehension.data.questionbank;

import java.io.Serializable;
import org.jetbrains.annotations.NotNull;

public record SearchIterationData(@NotNull SearchQuality quality, @NotNull Integer limit,
                                  @NotNull Integer found) implements Serializable {
}
