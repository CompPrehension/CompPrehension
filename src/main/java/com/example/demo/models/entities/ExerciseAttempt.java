package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.AttemptStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseAttempt")
public class ExerciseAttempt {
    //TODO: Нужен ли здесь язык студента
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private AttemptStatus attemptStatus;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "exerciseAttempt")
    private List<QuestionAttempt> questionAttempts;
}
