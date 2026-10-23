package org.vstu.compprehension.services;

import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.enums.Language;

import java.util.Optional;

/**
 * Текущий пользователь.
 */
public interface UserDataService {
    UserData getCurrentUser();

    void setLanguage(Language language);

    default Optional<UserData> tryGetCurrentUser() {
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
