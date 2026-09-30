package org.vstu.compprehension.data.lti;

import org.jetbrains.annotations.NotNull;

/**
 * Откуда брать участников курса: адрес списка NRPS и инструмент LMS, через который он получен.
 */
public record LtiCourseMembershipSourceData(
        long courseId,
        long educationResourceId,
        @NotNull String issuer,
        @NotNull String clientId,
        @NotNull String membershipsUrl) {
}
