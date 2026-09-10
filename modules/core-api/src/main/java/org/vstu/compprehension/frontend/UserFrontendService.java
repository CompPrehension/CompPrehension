package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.UserInfoDto;

import java.util.Optional;

public interface UserFrontendService {
    long getCurrentUserId();

    Optional<Language> tryGetCurrentUserLanguage();

    @NotNull Language getCurrentUserLanguage();

    void setLanguage(@NotNull Language language);

    @NotNull UserInfoDto getCurrentUserInfo();
}
