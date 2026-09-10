package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;

public record CourseSummaryData(
        long id,
        @NotNull String name,
        long educationResourceId,
        @NotNull String educationResourceUrl) {
}
