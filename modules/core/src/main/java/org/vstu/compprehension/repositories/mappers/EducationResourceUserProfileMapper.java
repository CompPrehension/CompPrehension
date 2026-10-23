package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserAccountUpdateData;
import org.vstu.compprehension.entities.external_system.EducationResourceUserEntity;
import org.vstu.compprehension.mappers.UpdateMapper;

@Component
class EducationResourceUserProfileMapper implements UpdateMapper<UserAccountUpdateData, EducationResourceUserEntity> {

    @Override
    public void apply(@NotNull UserAccountUpdateData source, @NotNull EducationResourceUserEntity destination) {
        destination.setFullName(source.fullName());
        destination.setEmail(source.email());
    }
}
