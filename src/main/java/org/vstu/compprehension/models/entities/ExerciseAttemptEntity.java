package org.vstu.compprehension.models.entities;

import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseAttempt")
public class ExerciseAttemptEntity {
    //TODO: Нужен ли здесь язык студента
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    private AttemptStatus attemptStatus;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "exerciseAttempt", fetch = FetchType.LAZY)
    private List<QuestionEntity> questions;
}
