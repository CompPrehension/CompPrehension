package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.businesslogic.user.SavedUserContext;
import org.vstu.compprehension.models.businesslogic.user.UserContext;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Component
@RequestScope
@Qualifier("basicAuthentication")
public class BasicAuthenticationUserContextAdapter implements UserContext {
    private final UserContext savedUser;

    @Autowired
    public BasicAuthenticationUserContextAdapter(UserService userService, HttpServletRequest request) throws Exception {
        var session = request.getSession();
        var previoslySavedInfo = session.getAttribute("currentUserInfo");
        if (previoslySavedInfo != null) {
            savedUser = (UserContext)previoslySavedInfo;
            return;
        }

        savedUser = new SavedUserContext(userService.getCurrentUser());
        session.setAttribute("currentUserInfo", savedUser);
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

    @NotNull
    @Override
    public Set<Role> getRoles() {
        return savedUser.getRoles();
    }
}
