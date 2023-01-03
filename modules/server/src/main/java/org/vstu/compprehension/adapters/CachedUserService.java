package org.vstu.compprehension.adapters;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.UserEntity;

import javax.annotation.Nullable;

public class CachedUserService implements UserService {
    private final UserService decoratee;

    private @Nullable UserEntity cachedCurrentUser;
    private @Nullable Object cachedCurrentUserPrincipal;

    public CachedUserService(UserService decoratee) {
        this.decoratee = decoratee;
    }

    @Override
    public UserEntity getCurrentUser() throws Exception {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (cachedCurrentUserPrincipal == principal && cachedCurrentUser != null) {
            return cachedCurrentUser;
        }

        cachedCurrentUser = decoratee.getCurrentUser();
        cachedCurrentUserPrincipal = principal;

        return cachedCurrentUser;
    }
}
