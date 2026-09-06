package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.data.CurrentUserData;
import org.vstu.compprehension.models.entities.EnumData.Language;

import java.util.Optional;

/**
 * Текущий пользователь.
 * <p>
 * Отдаёт данные, а не {@code UserEntity}: вызывающим нужны идентификатор, язык и пара
 * полей для карточки, а сущность протекала в контроллеры вместе с ленивыми связями.
 */
public interface UserService {
    CurrentUserData getCurrentUser();

    void setLanguage(Language language);

    default Optional<CurrentUserData> tryGetCurrentUser() {
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
