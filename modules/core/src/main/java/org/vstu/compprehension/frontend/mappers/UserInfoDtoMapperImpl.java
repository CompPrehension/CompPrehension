package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.permission.UserPermissionsData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.frontend.dto.UserInfoDto;
import org.vstu.compprehension.frontend.dto.UserPermissionsDto;

import java.util.Objects;

@Component
class UserInfoDtoMapperImpl implements UserInfoDtoMapper {

    @Override
    public @NotNull UserInfoDto map(@NotNull UserData user, @NotNull UserPermissionsData permissions) {
        String displayName = Objects.requireNonNullElse(user.firstName(), "");
        return UserInfoDto.builder()
                .id(user.id())
                .displayName(displayName)
                .email(user.email())
                .language(user.language().toLocaleString())
                .permissions(map(permissions))
                .build();
    }

    private @NotNull UserPermissionsDto map(@NotNull UserPermissionsData source) {
        return new UserPermissionsDto(source.canViewGlobalPool(), source.isLtiMode(), source.canRegisterLms());
    }
}
