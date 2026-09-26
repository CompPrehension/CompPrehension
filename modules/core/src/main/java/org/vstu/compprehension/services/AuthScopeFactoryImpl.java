package org.vstu.compprehension.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.businesslogic.auth.AuthScope;
import org.vstu.compprehension.businesslogic.auth.PermissionScope;
import org.vstu.compprehension.repositories.data.CourseDataRepository;
import org.vstu.compprehension.repositories.data.ExerciseDataRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
class AuthScopeFactoryImpl implements AuthScopeFactory {

    private final CourseEducationResourceCache educationResources;
    private final CourseDataRepository courses;
    private final ExerciseDataRepository exercises;

    public @NotNull AuthScope root() {
        return AuthScope.of(PermissionScope.root());
    }

    public @NotNull AuthScope global() {
        return AuthScope.of(PermissionScope.global(), PermissionScope.root());
    }

    public @NotNull AuthScope course(long courseId) {
        return anyOfCourses(List.of(courseId));
    }

    public @NotNull AuthScope anyOfCourses(Collection<Long> courseIds) {
        var distinctCourseIds = new LinkedHashSet<>(courseIds);
        var scopes = new ArrayList<PermissionScope>();
        for (Long courseId : distinctCourseIds) {
            scopes.add(PermissionScope.course(courseId));
        }
        for (Long eduResId : educationResources.educationResourceIdsOf(distinctCourseIds)) {
            scopes.add(PermissionScope.educationResource(eduResId));
        }
        scopes.add(PermissionScope.root());
        return new AuthScope(scopes);
    }

    public @NotNull AuthScope exercise(long exerciseId, @Nullable Long courseId) {
        if (courseId == null) {
            if (!exercises.getById(exerciseId).isPublic()) {
                throw new IllegalStateException("exercise_not_in_global_pool");
            }
            return global();
        }
        if (!courses.isExerciseInCourse(exerciseId, courseId)) {
            throw new IllegalStateException(String.format(
                    "There is no relation between the course (id=%s) and the exercise (id=%s)",
                    courseId, exerciseId));
        }
        return course(courseId);
    }
}
