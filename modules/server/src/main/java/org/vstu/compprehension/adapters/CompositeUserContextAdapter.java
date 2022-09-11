package org.vstu.compprehension.adapters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.businesslogic.user.SavedUserContext;
import org.vstu.compprehension.models.businesslogic.user.UserContext;
import org.vstu.compprehension.models.entities.EnumData.Language;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
@RequestScope
@Primary
public class CompositeUserContextAdapter implements UserContext {
    private final UserContext currentCtx;

    @Autowired
    public CompositeUserContextAdapter(UserService userService, HttpServletRequest request) throws Exception {
        var session = request.getSession();
        var previoslySavedInfo = session.getAttribute("currentUserInfo");
        if (previoslySavedInfo != null) {
            currentCtx = (UserContext)previoslySavedInfo;
            return;
        }

        var ltiInfo = request.getSession().getAttribute("ltiSessionInfo");
        if (ltiInfo != null) {
            var userInfo = userService.createOrUpdateFromLti((Map<String, String>)ltiInfo);
            currentCtx = new SavedUserContext(userInfo);
            session.setAttribute("currentUserInfo", currentCtx);
            return;
        }

        var userInfo = userService.createOrUpdateFromAuthentication();
        currentCtx = new SavedUserContext(userInfo);
        session.setAttribute("currentUserInfo", currentCtx);
    }

    @NotNull
    @Override
    public Long getId() {
        return currentCtx.getId();
    }

    @Nullable
    @Override
    public String getDisplayName() {
        return currentCtx.getDisplayName();
    }

    @Nullable
    @Override
    public String getEmail() {
        return currentCtx.getEmail();
    }

    @Nullable
    @Override
    public Language getPreferredLanguage() {
        return currentCtx.getPreferredLanguage();
    }
}
