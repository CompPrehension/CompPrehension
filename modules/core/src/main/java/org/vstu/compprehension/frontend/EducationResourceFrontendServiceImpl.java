package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.services.EducationResourceService;

import java.util.Optional;

@Component
public class EducationResourceFrontendServiceImpl implements EducationResourceFrontendService {
    private final EducationResourceService educationResourceService;

    public EducationResourceFrontendServiceImpl(EducationResourceService educationResourceService) {
        this.educationResourceService = educationResourceService;
    }

    @Override
    public Optional<Long> findIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type) {
        return educationResourceService.findIdByUrlAndType(url, type);
    }

    @Override
    public long getOrCreateTrustedId(@NotNull String url, @NotNull EducationResourceType type) {
        return educationResourceService.getOrCreateTrustedId(url, type);
    }
}
