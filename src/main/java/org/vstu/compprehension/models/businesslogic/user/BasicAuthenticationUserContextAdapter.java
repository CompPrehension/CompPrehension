package org.vstu.compprehension.models.businesslogic.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;

@Component
@SessionScope
@Qualifier("basicAuthentication")
public class BasicAuthenticationUserContextAdapter implements UserContext {
    private final SavedUserContext savedUser;

    @Autowired
    public BasicAuthenticationUserContextAdapter(UserService userService) throws Exception {
        savedUser = new SavedUserContext(userService.createOrUpdateFromAuthentication());
    }

    @NotNull
    @Override
    public Long getId() {
        return savedUser.getId();
    }

    @Nullable
    @Override
    public String getDisplayName() {
        return savedUser.getDisplayName();
    }

    @Nullable
    @Override
    public String getEmail() {
        return savedUser.getEmail();
    }

    @Nullable
    @Override
    public Language getPreferredLanguage() {
        return savedUser.getPreferredLanguage();
    }
}
