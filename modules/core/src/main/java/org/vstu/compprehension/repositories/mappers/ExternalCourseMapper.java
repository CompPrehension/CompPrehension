package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.ExternalCourseData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;
import org.vstu.compprehension.repositories.entity.CourseRepository.CourseView;

@Component
class ExternalCourseMapper implements Mapper<CourseView, ExternalCourseData> {

    @Override
    public @NotNull ExternalCourseData map(@NotNull CourseView source) {
        long id = source.getId();
        String owner = "course " + id;
        return new ExternalCourseData(
                id,
                Strict.required(source.getName(), "name", owner),
                Strict.required(source.getExternalCourseId(), "externalCourseId", owner));
    }
}
