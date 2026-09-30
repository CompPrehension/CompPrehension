package org.vstu.compprehension.entities.external_system;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "education_resource",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_education_resource_url",
        columnNames = {"url"}
    )
)
public class EducationResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_status", nullable = false, length = 32)
    @ColumnDefault("'UNTRUSTED'")
    private EducationResourceTrustStatus trustStatus;
}
