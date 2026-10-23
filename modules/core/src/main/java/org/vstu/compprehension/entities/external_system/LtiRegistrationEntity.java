package org.vstu.compprehension.entities.external_system;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.enums.LtiRegistrationMethod;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "lti_registration",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_lti_registration_issuer_client",
        columnNames = {"issuer", "client_id"}
    ),
    indexes = @Index(name = "ix_lti_registration_education_resource", columnList = "education_resource_id")
)
public class LtiRegistrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "education_resource_id", nullable = false)
    private EducationResourceEntity educationResource;

    @Column(name = "issuer", nullable = false, length = 512)
    private String issuer;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "deployment_id")
    private String deploymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private LtiRegistrationMethod method;

    @Column(name = "authorization_endpoint", nullable = false, length = 1024)
    private String authorizationEndpoint;

    @Column(name = "token_endpoint", nullable = false, length = 1024)
    private String tokenEndpoint;

    @Column(name = "jwks_uri", length = 1024)
    private String jwksUri;

    /** Открытый ключ LMS (X.509 DER в base64) для LMS без JWKS. */
    @Column(name = "platform_public_key", length = 2048)
    private String platformPublicKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
