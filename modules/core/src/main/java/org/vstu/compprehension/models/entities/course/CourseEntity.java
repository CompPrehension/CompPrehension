package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "Course")
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "education_resource_id", nullable = false)
    private EducationResourceEntity educationResources;

    @OneToMany(mappedBy = "course")
    private List<ExerciseEntity> exercises;
}
