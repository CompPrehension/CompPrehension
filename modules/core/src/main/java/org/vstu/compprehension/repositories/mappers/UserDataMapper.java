package org.vstu.compprehension.repositories.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserData;

/** Учётная запись в карточку пользователя: наружу уходит она, а не запись целиком. */
@Component
class UserDataMapper implements Mapper<UserAccountData, UserData> {

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
