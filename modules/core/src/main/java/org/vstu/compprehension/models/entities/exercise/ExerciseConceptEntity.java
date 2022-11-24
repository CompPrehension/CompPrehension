package org.vstu.compprehension.models.entities.exercise;

import lombok.Builder;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseConcepts")
public class ExerciseConceptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Column(name = "concept_name", nullable = false)
    private String conceptName;

    @Enumerated(EnumType.ORDINAL)
    private RoleInExercise roleInExercise;
}
