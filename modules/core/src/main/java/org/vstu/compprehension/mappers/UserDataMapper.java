package org.vstu.compprehension.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.data.user.UserAccountData;

@Component
public class UserDataMapper {

    public static @NotNull UserData toCurrentUser(@NotNull UserAccountData account) {
        return new UserData(
                account.id(),
                account.firstName(),
                account.lastName(),
                account.email(),
                account.language());
    }
}
