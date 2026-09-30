package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;

public record EducationResourceData(
        long id,
        @NotNull String url,
        @NotNull EducationResourceTrustStatus trustStatus) {
}
