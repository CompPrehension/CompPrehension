package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.EducationResourceType;

import java.util.Optional;

public interface EducationResourceFrontendService {
    Optional<Long> findIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type);

    long getOrCreateTrustedId(@NotNull String url, @NotNull EducationResourceType type);
}
