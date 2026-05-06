package org.vstu.compprehension.models.entities.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

@Entity
@Getter
@NoArgsConstructor
@IdClass(ExerciseCourseLinkId.class)
@Table(name = "exercise_course_link")
public class ExerciseCourseLinkEntity {

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    public ExerciseCourseLinkEntity(CourseEntity course, ExerciseEntity exercise) {
        this.exercise = exercise;
        this.course = course;
    }
}
