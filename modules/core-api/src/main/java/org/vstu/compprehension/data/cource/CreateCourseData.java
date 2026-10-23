package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CreateCourseData(long educationResourceId, @NotNull String externalCourseId, @Nullable String name) {
}
