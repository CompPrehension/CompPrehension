package org.vstu.compprehension.entities.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.vstu.compprehension.entities.ExerciseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@IdClass(ExerciseCourseLinkId.class)
@Table(name = "exercise_course_link")
public class ExerciseCourseLinkEntity {

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime linkedAt;

    public ExerciseCourseLinkEntity(CourseEntity course, ExerciseEntity exercise) {
        this.exercise = exercise;
        this.course = course;
    }
}
