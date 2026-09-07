package org.vstu.compprehension.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.CurrentUserData;
import org.vstu.compprehension.data.user.UserAccountData;

/**
 * Учётная запись в карточку текущего пользователя.
 * <p>
 * Данные в данные: сущность превращает в {@link UserAccountData} слой доступа к данным,
 * а здесь из полной записи остаётся то, что показывают в интерфейсе. Без зависимостей,
 * как и все мапперы.
 */
@Component
public class UserDataMapper {

    public static @NotNull CurrentUserData toCurrentUser(@NotNull UserAccountData account) {
        return new CurrentUserData(
                account.id(),
                account.firstName(),
                account.lastName(),
                account.email(),
                account.language());
    }
}
