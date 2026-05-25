package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "education_resource",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_education_resource_url_type",
        columnNames = {"url", "type"}
    )
)
public class EducationResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private EducationResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_status", nullable = false, length = 32)
    private EducationResourceTrustStatus trustStatus;

    public EducationResourceEntity(String url, EducationResourceType type) {
        this.url = url;
        this.type = type;
        this.trustStatus = EducationResourceTrustStatus.UNTRUSTED;
    }
}
