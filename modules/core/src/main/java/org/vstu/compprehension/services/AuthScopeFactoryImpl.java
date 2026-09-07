package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
class AuthScopeFactoryImpl implements AuthScopeFactory {

    private final CourseEducationResourceCache educationResources;

    public @NotNull AuthScope global() {
        return AuthScope.of(PermissionScope.global());
    }

    public @NotNull AuthScope educationResource(long educationResourceId) {
        return AuthScope.of(PermissionScope.educationResource(educationResourceId));
    }

    public @NotNull AuthScope course(long courseId) {
        return anyOfCourses(List.of(courseId));
    }

    public @NotNull AuthScope courseOrGlobal(@Nullable Long courseId) {
        return courseId == null ? global() : course(courseId);
    }

    public @NotNull AuthScope anyOfCourses(Collection<Long> courseIds) {
        var distinctCourseIds = new LinkedHashSet<>(courseIds);
        if (distinctCourseIds.isEmpty()) {
            return new AuthScope(List.of());
        }
        var scopes = new ArrayList<PermissionScope>();
        distinctCourseIds.forEach(courseId -> scopes.add(PermissionScope.course(courseId)));
        educationResources.educationResourceIdsOf(distinctCourseIds)
                .forEach(eduResId -> scopes.add(PermissionScope.educationResource(eduResId)));
        return new AuthScope(scopes);
    }
}
