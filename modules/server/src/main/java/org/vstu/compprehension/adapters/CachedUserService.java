package org.vstu.compprehension.adapters;

import org.springframework.security.core.context.SecurityContextHolder;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.services.UserDataService;
import org.vstu.compprehension.enums.Language;

import javax.annotation.Nullable;

public class CachedUserService implements UserDataService {
    private final UserDataService decoratee;

    private @Nullable UserData cachedCurrentUser;
    private @Nullable Object cachedCurrentUserPrincipal;

    public CachedUserService(UserDataService decoratee) {
        this.decoratee = decoratee;
    }

    @Override
    public UserData getCurrentUser() {
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
