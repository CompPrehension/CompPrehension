package org.vstu.compprehension.jobs.moodlesync;

import org.vstu.compprehension.models.entities.EnumData.Role;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Маппинг Moodle capabilityName → Role на стороне трейнера.
 * Приоритет фиксированный: чем выше capabilityName, тем выше роль.
 */
public final class CapabilityMapper {
    public static final String CAP_COURSE_UPDATE = "moodle/course:update";
    public static final String CAP_GRADE_VIEWALL = "moodle/grade:viewall";
    public static final String CAP_LTI_VIEW = "mod/lti:view";

    private CapabilityMapper() {
    }

    public static Set<String> allRelevantCourseCapabilities() {
        return new LinkedHashSet<>(Set.of(
                CAP_COURSE_UPDATE,
                CAP_GRADE_VIEWALL,
                CAP_LTI_VIEW
        ));
    }

    public static Role deriveCourseRole(Set<String> caps) {
        // Order matters: an editing teacher holds both course:update and grade:viewall and must
        // resolve to TEACHER first; a Non-editing teacher holds grade:viewall (but not course:update)
        // -> ASSISTANT; a student holds neither -> STUDENT.
        if (caps.contains(CAP_COURSE_UPDATE)) return Role.TEACHER;
        if (caps.contains(CAP_GRADE_VIEWALL)) return Role.ASSISTANT;
        if (caps.contains(CAP_LTI_VIEW)) return Role.STUDENT;
        return null;
    }
}
