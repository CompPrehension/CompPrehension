package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "cource")
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "education_resource_id", nullable = false)
    private EducationResourceEntity educationResources;

    @ToString.Exclude
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<ExerciseEntity> exercises;
}
