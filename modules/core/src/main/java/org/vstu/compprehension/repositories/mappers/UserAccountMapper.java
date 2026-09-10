package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.entities.UserEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;

@Component
class UserAccountMapper implements Mapper<UserEntity, UserAccountData> {

    @Override
    public @NotNull UserAccountData map(@NotNull UserEntity source) {
        long id = Strict.required(source.getId(), "id", "user");
        String owner = "user " + id;
        return new UserAccountData(
                id,
                source.getFirstName(),
                source.getLastName(),
                Strict.required(source.getEmail(), "email", owner),
                Strict.required(source.getPreferred_language(), "preferred_language", owner),
                source.getExternalId(),
                source.getExternalUserId());
    }
}
