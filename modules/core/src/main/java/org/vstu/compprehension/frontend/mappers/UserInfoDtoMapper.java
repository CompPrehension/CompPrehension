package org.vstu.compprehension.frontend.mappers;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.data.permission.UserPermissionsData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.frontend.dto.UserInfoDto;
import org.vstu.compprehension.mappers.Mapping;

public interface UserInfoDtoMapper extends Mapping {

    @NotNull UserInfoDto map(@NotNull UserData user, @NotNull UserPermissionsData permissions);
}
