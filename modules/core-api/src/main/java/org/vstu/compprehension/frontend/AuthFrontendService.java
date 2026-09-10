package org.vstu.compprehension.frontend;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.Permission;

public interface AuthFrontendService {
    void ensureAuthorized(long userId, Permission permission, AuthScope scope);

    AuthScope global();

    AuthScope course(long courseId);

    AuthScope courseOrGlobal(@Nullable Long courseId);
}
