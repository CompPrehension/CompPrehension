package org.vstu.compprehension.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import java.util.Collection;

public interface AuthScopeFactory {
    @NotNull AuthScope global();
    
    @NotNull AuthScope educationResource(long educationResourceId);

    @NotNull AuthScope course(long courseId);

    @NotNull AuthScope courseOrGlobal(@Nullable Long courseId);

    @NotNull AuthScope anyOfCourses(Collection<Long> courseIds);
}
