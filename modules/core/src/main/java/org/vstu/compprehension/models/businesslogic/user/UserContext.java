package org.vstu.compprehension.models.businesslogic.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;

import java.util.Set;

public interface UserContext {
    @NotNull Long getId();
    @Nullable String getDisplayName();
    @Nullable String getEmail();
    @Nullable Language getPreferredLanguage();
    @NotNull Set<Role> getRoles();
}
