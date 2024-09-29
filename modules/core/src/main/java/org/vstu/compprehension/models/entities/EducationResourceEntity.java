package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "educational_resource")
public class EducationResourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false)
    private String url;

    @ToString.Exclude
    @OneToMany(mappedBy = "educationResources", fetch = FetchType.LAZY)
    private List<CourseEntity> courses;
}
