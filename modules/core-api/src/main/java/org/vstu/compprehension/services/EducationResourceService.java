package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;

import java.util.Optional;

public interface EducationResourceService {
    @NotNull Optional<Long> findTrustedIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type);

    @NotNull EducationResourceData getOrCreate(@NotNull String url, @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus);
}
