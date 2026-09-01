package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "course",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_course_edu_resource_external_course",
        columnNames = {"education_resource_id", "external_course_id"}
    )
)
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "external_course_id", length = 255)
    private String externalCourseId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "education_resource_id", nullable = false)
    private EducationResourceEntity educationResource;

    public CourseEntity(String externalCourseId, String name, EducationResourceEntity educationResource) {
        this.externalCourseId = externalCourseId;
        this.name = name;
        this.educationResource = educationResource;
    }
}
