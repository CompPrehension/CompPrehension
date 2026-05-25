package org.vstu.compprehension.integration.moodle;

import java.util.Set;

public record CourseCapabilityRequest(String externalCourseId, Set<String> capabilities) {
}
