package org.vstu.compprehension.businesslogic.lti;


import org.vstu.compprehension.enums.EducationResourceType;

public record LtiContext(
        String lineitemUrl,
        LtiCourseContext course,
        String lmsUrl,
        String lmsName,
        EducationResourceType lmsType,
        Long exerciseId
) {
}
