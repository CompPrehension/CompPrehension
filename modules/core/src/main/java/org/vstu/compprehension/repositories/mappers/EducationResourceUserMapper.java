package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.EducationResourceUserData;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;
import org.vstu.compprehension.repositories.entity.EducationResourceUserRepository.EducationResourceUserView;

@Component
class EducationResourceUserMapper implements Mapper<EducationResourceUserView, EducationResourceUserData> {

    @Override
    public @NotNull EducationResourceUserData map(@NotNull EducationResourceUserView source) {
        long educationResourceId = Strict.required(
                source.getEducationResourceId(), "educationResourceId", "education resource user");
        String owner = "user of education resource " + educationResourceId;
        return new EducationResourceUserData(
                Strict.required(source.getUserId(), "userId", owner),
                educationResourceId,
                Strict.required(source.getExternalId(), "externalId", owner));
    }
}
