package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.vstu.compprehension.models.entities.EnumData.AttemptStatus;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.Date;
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ToString.Exclude
    @OneToMany(mappedBy = "exerciseAttempt", fetch = FetchType.LAZY)
    private List<QuestionEntity> questions;
}
