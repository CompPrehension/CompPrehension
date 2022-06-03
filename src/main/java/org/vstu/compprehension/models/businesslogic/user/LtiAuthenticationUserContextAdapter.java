package org.vstu.compprehension.models.businesslogic.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
@SessionScope
@Qualifier("ltiAuthentication")
public class LtiAuthenticationUserContextAdapter implements UserContext {
    private final SavedUserContext savedUser;

    @Autowired
    public LtiAuthenticationUserContextAdapter(UserService userService, HttpServletRequest request) throws Exception {
        savedUser = new SavedUserContext(userService.createOrUpdateFromLti((Map<String, String>)request.getSession().getAttribute("ltiSessionInfo")));
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
