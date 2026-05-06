package org.vstu.compprehension.models.businesslogic.lti;

import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;

public record LtiContext(
        String lineitemUrl,
        LtiCourseContext course,
        String lmsUrl,
        String lmsName,
        EducationResourceType lmsType,
        Long exerciseId
) {
}
