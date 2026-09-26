package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.frontend.dto.EducationResourceDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class EducationResourceDtoMapper implements Mapper<EducationResourceData, EducationResourceDto> {

    @Override
    public @NotNull EducationResourceDto map(@NotNull EducationResourceData source) {
        return new EducationResourceDto(
                source.id(),
                source.url(),
                source.type(),
                source.trustStatus());
    }
}
