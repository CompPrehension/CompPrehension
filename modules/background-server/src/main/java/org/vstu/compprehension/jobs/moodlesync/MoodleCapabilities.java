package org.vstu.compprehension.jobs.moodlesync;

import org.vstu.compprehension.businesslogic.auth.AuthObjects.SystemRole;
import org.vstu.compprehension.businesslogic.auth.Role;

import java.util.LinkedHashSet;
import java.util.Set;


/** Каталог moodle-прав и вывод роли по ним. Не маппер типов: имя это подчёркивает. */
public final class MoodleCapabilities {
    public static final String CAP_COURSE_UPDATE = "moodle/course:update";
    public static final String CAP_GRADE_VIEWALL = "moodle/grade:viewall";
    public static final String CAP_LTI_VIEW = "mod/lti:view";

    private MoodleCapabilities() {
    }

    public static Set<String> allRelevantCourseCapabilities() {
        return new LinkedHashSet<>(Set.of(
                CAP_COURSE_UPDATE,
                CAP_GRADE_VIEWALL,
                CAP_LTI_VIEW
        ));
    }

    public static Role deriveCourseRole(Set<String> caps) {
        if (caps.contains(CAP_COURSE_UPDATE)) return SystemRole.TEACHER;
        if (caps.contains(CAP_GRADE_VIEWALL)) return SystemRole.ASSISTANT;
        if (caps.contains(CAP_LTI_VIEW)) return SystemRole.STUDENT;
        return null;
    }
}
