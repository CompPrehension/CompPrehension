package org.vstu.compprehension.moodle.request;

import java.util.Set;

public record CourseCapabilityRequest(String externalCourseId, Set<String> capabilities) {
}
