package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.frontend.dto.EducationResourceDto;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.services.EducationResourceService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EducationResourceFrontendServiceImpl implements EducationResourceFrontendService {
    private final EducationResourceService educationResourceService;
    private final Mapper<EducationResourceData, EducationResourceDto> educationResourceMapper;

    @Override
    public Optional<Long> findIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type) {
        return educationResourceService.findIdByUrlAndType(url, type);
    }

    @Override
    public @NotNull EducationResourceDto getOrCreate(@NotNull String url, @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        return educationResourceMapper.map(educationResourceService.getOrCreate(url, type, trustStatus));
    }
}
