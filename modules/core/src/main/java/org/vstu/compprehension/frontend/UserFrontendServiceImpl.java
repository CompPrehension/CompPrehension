package org.vstu.compprehension.frontend;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.frontend.dto.UserInfoDto;
import org.vstu.compprehension.data.permission.UserPermissionsData;
import org.vstu.compprehension.services.LtiContextProvider;
import org.vstu.compprehension.services.UserDataService;
import org.vstu.compprehension.frontend.mappers.UserInfoDtoMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserFrontendServiceImpl implements UserFrontendService {
    private final UserDataService userService;
    private final AuthFrontendService authService;
    private final LtiContextProvider ltiContextProvider;
    private final UserInfoDtoMapper userInfoDtoMapper;

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
        var permissions = new UserPermissionsData(
                authService.canViewGlobalPool(user.id()),
                ltiContextProvider.getCurrentLtiContext().isPresent());
        return userInfoDtoMapper.map(user, permissions);
    }
}
