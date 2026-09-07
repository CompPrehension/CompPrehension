package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.EducationResourceType;

import java.util.Optional;

public interface EducationResourceService {
    @NotNull Optional<Long> findIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type);

    long getOrCreateTrustedId(@NotNull String url, @NotNull EducationResourceType type);
}
