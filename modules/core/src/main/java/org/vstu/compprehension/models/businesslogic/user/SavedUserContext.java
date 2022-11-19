package org.vstu.compprehension.models.businesslogic.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SavedUserContext implements UserContext {
    private final @NotNull UserEntity user;

    public SavedUserContext(@NotNull UserEntity user) {
        this.user = user;
    }

    @NotNull
    @Override
    public Long getId() {
        return user.getId();
    }

    private @Nullable String displayName;
    @Nullable
    @Override
    public String getDisplayName() {
        if (displayName != null)
            return displayName;

        displayName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
        return displayName;
    }

    @Nullable
    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Nullable
    @Override
    public Language getPreferredLanguage() {
        return user.getPreferred_language();
    }

    @NotNull
    @Override
    public Set<Role> getRoles() {
        return user.getRoles();
    }
}
