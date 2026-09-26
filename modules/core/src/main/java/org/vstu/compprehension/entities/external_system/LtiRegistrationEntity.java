package org.vstu.compprehension.entities.external_system;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lti_registration")
public class LtiRegistrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "education_resource_id", nullable = false, unique = true)
    private EducationResourceEntity educationResource;

    @Column(name = "issuer", nullable = false, unique = true, length = 512)
    private String issuer;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "deployment_id")
    private String deploymentId;

    @Column(name = "authorization_endpoint", nullable = false, length = 1024)
    private String authorizationEndpoint;

    @Column(name = "token_endpoint", nullable = false, length = 1024)
    private String tokenEndpoint;

    @Column(name = "jwks_uri", nullable = false, length = 1024)
    private String jwksUri;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
