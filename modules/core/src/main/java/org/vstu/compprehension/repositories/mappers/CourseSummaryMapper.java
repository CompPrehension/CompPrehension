package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.CourseSummaryData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.Strict;
import org.vstu.compprehension.repositories.entity.CourseRepository.CourseView;

/** Курс в объёме списка. */
@Component
class CourseSummaryMapper implements Mapper<CourseView, CourseSummaryData> {

    @Override
    public @NotNull CourseSummaryData map(@NotNull CourseView source) {
        String owner = "course " + source.getId();
        return new CourseSummaryData(
                source.getId(),
                Strict.required(source.getName(), "name", owner),
                source.getEducationResourceId(),
                Strict.required(source.getEducationResourceUrl(), "educationResourceUrl", owner));
    }
}
