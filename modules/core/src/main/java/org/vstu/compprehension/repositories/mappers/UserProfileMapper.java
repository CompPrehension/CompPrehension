package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserAccountUpdateData;
import org.vstu.compprehension.entities.UserEntity;
import org.vstu.compprehension.mappers.UpdateMapper;

@Component
class UserProfileMapper implements UpdateMapper<UserAccountUpdateData, UserEntity> {

    @Override
    public void apply(@NotNull UserAccountUpdateData source, @NotNull UserEntity destination) {
        destination.setEmail(source.email());
        destination.setFirstName(source.fullName());
        destination.setPreferred_language(source.language());
    }
}
