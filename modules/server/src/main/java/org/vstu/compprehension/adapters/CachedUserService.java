package org.vstu.compprehension.adapters;

import org.springframework.security.core.context.SecurityContextHolder;
import org.vstu.compprehension.data.user.CurrentUserData;
import org.vstu.compprehension.services.UserService;
import org.vstu.compprehension.data.enums.Language;

import javax.annotation.Nullable;

public class CachedUserService implements UserService {
    private final UserService decoratee;

    private @Nullable CurrentUserData cachedCurrentUser;
    private @Nullable Object cachedCurrentUserPrincipal;

    public CachedUserService(UserService decoratee) {
        this.decoratee = decoratee;
    }

    @Override
    public CurrentUserData getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (cachedCurrentUserPrincipal == principal && cachedCurrentUser != null) {
            return cachedCurrentUser;
        }

        cachedCurrentUser = decoratee.getCurrentUser();
        cachedCurrentUserPrincipal = principal;

        return cachedCurrentUser;
    }


    @Override
    public void setLanguage(Language language) {
        decoratee.setLanguage(language);

        // Update the cached user after setting the language
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        cachedCurrentUser = decoratee.getCurrentUser();
        cachedCurrentUserPrincipal = principal;
    }
}
