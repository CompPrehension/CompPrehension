package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.dto.EducationResourceDto;

import java.util.Optional;

public interface EducationResourceFrontendService {
    @NotNull Optional<Long> findTrustedIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type);

    @NotNull EducationResourceDto getOrCreate(@NotNull String url, @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus);
}
