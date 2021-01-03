package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.RoleInExercise;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseLaws")
public class ExerciseLawsEntity {
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
