package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.QuestionType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseQuestionType")
public class ExerciseQuestionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "questionType")
    @Enumerated(EnumType.ORDINAL)
    private QuestionType questionType;

}
