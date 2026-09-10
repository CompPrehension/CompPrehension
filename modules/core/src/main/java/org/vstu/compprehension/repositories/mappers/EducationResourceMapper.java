package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.cource.EducationResourceData;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class EducationResourceMapper implements Mapper<EducationResourceEntity, EducationResourceData> {

    @Override
    public @NotNull EducationResourceData map(@NotNull EducationResourceEntity source) {
        long id = Strict.required(source.getId(), "id", "education resource " + source.getUrl());
        String owner = "education resource " + id;
        return new EducationResourceData(
                id,
                Strict.required(source.getUrl(), "url", owner),
                Strict.required(source.getType(), "type", owner),
                Strict.required(source.getTrustStatus(), "trustStatus", owner));
    }
}
