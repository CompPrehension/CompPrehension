package org.vstu.compprehension.models.entities;

import jakarta.persistence.Entity;
import org.vstu.compprehension.models.entities.EnumData.QuestionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseQuestionType")
public class ExerciseQuestionTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Column(name = "questionType")
    @Enumerated(EnumType.ORDINAL)
    private QuestionType questionType;

    public ExerciseQuestionTypeEntity(ExerciseEntity exercise, QuestionType questionType) {
        this.exercise = exercise;
        this.questionType = questionType;
    }
}
