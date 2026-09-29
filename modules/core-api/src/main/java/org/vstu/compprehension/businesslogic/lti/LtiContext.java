package org.vstu.compprehension.businesslogic.lti;


public record LtiContext(
        String lineitemUrl,
        String issuer,
        String clientId,
        long educationResourceId,
        LtiCourseContext course,
        Long exerciseId
) {
}
