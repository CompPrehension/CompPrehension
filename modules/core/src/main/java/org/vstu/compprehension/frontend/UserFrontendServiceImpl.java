package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.UserInfoDto;
import org.vstu.compprehension.services.ExercisePermissionDataService;
import org.vstu.compprehension.services.UserDataService;
import org.vstu.compprehension.frontend.mappers.LegacyDtoMappers;

import java.util.Optional;

@Component
public class UserFrontendServiceImpl implements UserFrontendService {
    private final UserDataService userService;
    private final ExercisePermissionDataService exercisePermissionService;

    public UserFrontendServiceImpl(UserDataService userService, ExercisePermissionDataService exercisePermissionService) {
        this.userService = userService;
        this.exercisePermissionService = exercisePermissionService;
    }

    @Override
    public long getCurrentUserId() {
        return userService.getCurrentUser().id();
    }

    @Override
    public Optional<Language> tryGetCurrentUserLanguage() {
        return userService.tryGetCurrentUser().map(UserData::language);
    }

    @Override
    public @NotNull Language getCurrentUserLanguage() {
        return userService.getCurrentUser().language();
    }

    @Override
    public void setLanguage(@NotNull Language language) {
        userService.setLanguage(language);
    }

    @Override
    public @NotNull UserInfoDto getCurrentUserInfo() {
        var user = userService.getCurrentUser();
        return LegacyDtoMappers.toDto(user, exercisePermissionService.ofUser(user.id()));
    }
}
