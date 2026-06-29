package org.vstu.compprehension.jobs.moodlesync;

import org.vstu.compprehension.models.entities.EnumData.Role;

import java.util.LinkedHashSet;
import java.util.Set;


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
        if (caps.contains(CAP_COURSE_UPDATE)) return Role.TEACHER;
        if (caps.contains(CAP_GRADE_VIEWALL)) return Role.ASSISTANT;
        if (caps.contains(CAP_LTI_VIEW)) return Role.STUDENT;
        return null;
    }
}
