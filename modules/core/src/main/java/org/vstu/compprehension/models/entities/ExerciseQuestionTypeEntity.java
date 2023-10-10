package org.vstu.compprehension.models.entities;

import jakarta.persistence.Entity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseQuestionType")
public class ExerciseQuestionTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Column(name = "questionType")
    @Enumerated(EnumType.ORDINAL)
    private QuestionType questionType;

}
