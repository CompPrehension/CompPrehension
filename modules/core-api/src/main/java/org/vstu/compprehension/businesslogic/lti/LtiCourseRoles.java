package org.vstu.compprehension.businesslogic.lti;

import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.businesslogic.auth.Role;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Роль в курсе, которую получает участник курса LMS по своим LTI-ролям.
 */
public final class LtiCourseRoles {
    private static final Set<String> TEACHER_ROLES = Set.of("Instructor", "ContentDeveloper");
    private static final String ASSISTANT_ROLE = "TeachingAssistant";

    private LtiCourseRoles() {
    }

    /** LTI-роли — полные URI ({@code …/membership#Instructor}) или только имена после {@code #}. */
    public static @NotNull Role resolveCourseRole(@NotNull Collection<String> ltiRoles) {
        Set<String> names = ltiRoles.stream()
                .map(role -> role.substring(role.lastIndexOf('#') + 1))
                .collect(Collectors.toSet());
        // Ассистент — уточнение роли Instructor, и LMS присылает вместе с ним саму Instructor.
        if (names.contains(ASSISTANT_ROLE)) {
            return SystemRole.ASSISTANT;
        }
        if (names.stream().anyMatch(TEACHER_ROLES::contains)) {
            return SystemRole.TEACHER;
        }
        return SystemRole.STUDENT;
    }
}
