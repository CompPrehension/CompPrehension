package org.vstu.compprehension.models.entities.educationresource;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "education_resource",
        uniqueConstraints = @UniqueConstraint(columnNames = {"url", "type"}))
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 64)
@Getter
@Setter
@NoArgsConstructor
public abstract class EducationResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, insertable = false, updatable = false, length = 64)
    private EducationResourceType type;
}
