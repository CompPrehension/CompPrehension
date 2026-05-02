package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "exercise_course",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_exercise_course_pair",
        columnNames = {"exercise_id", "course_id"}
    )
)
public class ExerciseCourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public ExerciseCourseEntity(CourseEntity course, ExerciseEntity exercise) {
        this.exercise = exercise;
        this.course = course;
    }

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "exercise_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_exercise_course_exercise")
    )
    private ExerciseEntity exercise;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "course_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_exercise_course_course")
    )
    private CourseEntity course;
}
