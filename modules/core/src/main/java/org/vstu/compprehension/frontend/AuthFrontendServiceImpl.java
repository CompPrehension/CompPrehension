package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.Permission;
import org.vstu.compprehension.services.AuthScopeFactory;
import org.vstu.compprehension.services.AuthService;

@Component
public class AuthFrontendServiceImpl implements AuthFrontendService {
    private final AuthService authService;
    private final AuthScopeFactory authScopeFactory;

    public AuthFrontendServiceImpl(AuthService authService, AuthScopeFactory authScopeFactory) {
        this.authService = authService;
        this.authScopeFactory = authScopeFactory;
    }

    @Override
    public void ensureAuthorized(long userId, Permission permission, AuthScope scope) {
        authService.ensureAuthorized(userId, permission, scope);
    }

    @Override
    public AuthScope global() {
        return authScopeFactory.global();
    }

    @Override
    public AuthScope course(long courseId) {
        return authScopeFactory.course(courseId);
    }

    @Override
    public AuthScope courseOrGlobal(@Nullable Long courseId) {
        return authScopeFactory.courseOrGlobal(courseId);
    }
}
