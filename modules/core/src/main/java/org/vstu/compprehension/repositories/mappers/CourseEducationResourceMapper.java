package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.CourseEducationResourceData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.CourseRepository.CourseView;

/** Ссылка курса на внешнюю систему. */
@Component
class CourseEducationResourceMapper implements Mapper<CourseView, CourseEducationResourceData> {

    @Override
    public @NotNull CourseEducationResourceData map(@NotNull CourseView source) {
        return new CourseEducationResourceData(source.getId(), source.getEducationResourceId());
    }
}
