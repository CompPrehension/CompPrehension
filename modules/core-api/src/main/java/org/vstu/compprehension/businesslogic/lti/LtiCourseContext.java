package org.vstu.compprehension.businesslogic.lti;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record LtiCourseContext(@NotNull String courseId, @Nullable String courseName) {
}
