package org.vstu.compprehension.models.businesslogic.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
@RequestScope
@Qualifier("ltiAuthentication")
public class LtiAuthenticationUserContextAdapter implements UserContext {
    private final UserContext savedUser;

    @Autowired
    public LtiAuthenticationUserContextAdapter(UserService userService, HttpServletRequest request) {
        var session = request.getSession();
        var previoslySavedInfo = session.getAttribute("currentUserInfo");
        if (previoslySavedInfo != null) {
            savedUser = (UserContext)previoslySavedInfo;
            return;
        }

        var ltiParams = request.getSession().getAttribute("ltiSessionInfo");
        if (ltiParams == null)
            throw new IllegalStateException("Couldn't find lti data");

        savedUser = new SavedUserContext(userService.createOrUpdateFromLti((Map<String, String>)ltiParams));
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
}
