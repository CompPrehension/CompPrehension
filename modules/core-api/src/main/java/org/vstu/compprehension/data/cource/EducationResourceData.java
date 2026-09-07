package org.vstu.compprehension.data.cource;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;

public record EducationResourceData(
        long id,
        @NotNull String url,
        @NotNull EducationResourceType type,
        @NotNull EducationResourceTrustStatus trustStatus) {
}
