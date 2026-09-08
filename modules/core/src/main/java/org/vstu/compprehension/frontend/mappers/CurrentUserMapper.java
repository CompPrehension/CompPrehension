package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserData;

@Component
class CurrentUserMapper implements Mapper<UserAccountData, UserData> {

    @Override
    public @NotNull UserData map(@NotNull UserAccountData source) {
        return new UserData(
                source.id(),
                source.firstName(),
                source.lastName(),
                source.email(),
                source.language());
    }
}
