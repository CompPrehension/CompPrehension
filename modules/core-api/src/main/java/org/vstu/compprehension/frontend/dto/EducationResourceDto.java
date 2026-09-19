package org.vstu.compprehension.frontend.dto;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;

public record EducationResourceDto(
        long id,
        @NotNull String url,
        @NotNull EducationResourceType type,
        @NotNull EducationResourceTrustStatus trustStatus) {
}
