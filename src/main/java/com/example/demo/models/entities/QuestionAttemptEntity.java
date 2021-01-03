package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "QuestionAttempt")
public class QuestionAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToMany(mappedBy = "questionAttempt", fetch = FetchType.LAZY)
    private List<InteractionEntity> interactions;


    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @ManyToOne
    @JoinColumn(name = "exerciseAttempt_id", nullable = false)
    private ExerciseAttemptEntity exerciseAttempt;
}
