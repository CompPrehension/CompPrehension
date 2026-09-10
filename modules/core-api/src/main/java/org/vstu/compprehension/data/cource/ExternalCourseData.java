package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;

public record ExternalCourseData(long id, @NotNull String name, @NotNull String externalCourseId) {
}
