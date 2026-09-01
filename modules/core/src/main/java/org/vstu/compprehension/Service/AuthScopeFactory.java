package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.models.businesslogic.auth.AuthScope;
import org.vstu.compprehension.models.entities.EnumData.PermissionScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Фабрика скоупов авторизации.
 */
@Service
@RequiredArgsConstructor
public class AuthScopeFactory {

    private final CourseEducationResourceCache educationResources;

    public AuthScope global() {
        return AuthScope.of(PermissionScope.global());
    }

    public AuthScope educationResource(long educationResourceId) {
        return AuthScope.of(PermissionScope.educationResource(educationResourceId));
    }

    /** Курс вместе с его образовательным ресурсом. */
    public AuthScope course(long courseId) {
        return anyOfCourses(List.of(courseId));
    }

    public AuthScope courseOrGlobal(@Nullable Long courseId) {
        return courseId == null ? global() : course(courseId);
    }

    /** Любой из курсов вместе с их образовательными ресурсами. */
    public AuthScope anyOfCourses(Collection<Long> courseIds) {
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
