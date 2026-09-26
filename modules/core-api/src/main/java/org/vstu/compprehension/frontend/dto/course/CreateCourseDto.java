package org.vstu.compprehension.frontend.dto.course;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CreateCourseDto(long educationResourceId, @NotNull String externalCourseId, @Nullable String name) {
}
