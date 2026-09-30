package org.vstu.compprehension.entities.external_system;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Участники LTI курса.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lti_course_membership")
public class LtiCourseMembershipEntity {
    @Id
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "issuer", nullable = false, length = 512)
    private String issuer;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "memberships_url", nullable = false, length = 1024)
    private String membershipsUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
