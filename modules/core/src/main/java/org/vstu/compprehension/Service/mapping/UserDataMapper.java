package org.vstu.compprehension.Service.mapping;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.data.CurrentUserData;
import org.vstu.compprehension.models.entities.UserEntity;

/** Перенос пользователя из сущности в данные. Без зависимостей, как и все мапперы. */
@Component
public class UserDataMapper {

    public static @NotNull CurrentUserData toCurrentUser(@NotNull UserEntity entity) {
        return new CurrentUserData(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPreferred_language());
    }
}
