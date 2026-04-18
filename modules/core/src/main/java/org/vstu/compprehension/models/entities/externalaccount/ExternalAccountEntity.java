package org.vstu.compprehension.models.entities.externalaccount;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.entities.educationresource.EducationResourceEntity;

@Entity
@Table(name = "external_account",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "education_resource_id"}))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 64)
@Getter
@Setter
@NoArgsConstructor
public abstract class ExternalAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "education_resource_id", nullable = false)
    private EducationResourceEntity educationResource;
}
