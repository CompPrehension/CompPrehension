package org.vstu.compprehension.models.entities.exercise;

import lombok.Builder;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseLaws")
public class ExerciseLawEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseEntity exercise;

    @Column(name = "law_name")
    private String lawName;

    @Enumerated(EnumType.ORDINAL)
    private RoleInExercise roleInExercise;
}
