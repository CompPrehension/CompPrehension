package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "QuestionAttempt")
public class QuestionAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToMany(mappedBy = "questionAttempt", fetch = FetchType.LAZY)
    private List<Interaction> interactions;


    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne
    @JoinColumn(name = "exerciseAttempt_id", nullable = false)
    private ExerciseAttempt exerciseAttempt;
}
