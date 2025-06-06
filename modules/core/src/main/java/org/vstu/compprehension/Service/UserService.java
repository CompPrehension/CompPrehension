package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.Optional;

public interface UserService {
    UserEntity getCurrentUser() throws Exception;
    void setLanguage(Language language) throws Exception;
    default Optional<UserEntity> tryGetCurrentUser() {
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
