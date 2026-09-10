package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.CourseSummaryData;
import org.vstu.compprehension.frontend.dto.course.CourseDto;
import org.vstu.compprehension.mappers.Mapper;

@Component
class CourseDtoMapper implements Mapper<CourseSummaryData, CourseDto> {

    @Override
    public @NotNull CourseDto map(@NotNull CourseSummaryData source) {
        return new CourseDto(
                source.id(),
                source.name(),
                source.educationResourceId(),
                source.educationResourceUrl());
    }
}
