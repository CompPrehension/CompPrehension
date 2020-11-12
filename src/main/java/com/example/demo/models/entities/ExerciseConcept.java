package com.example.demo.models.entities;

import com.example.demo.models.entities.EnumData.RoleInExercise;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "ExerciseConcepts")
public class ExerciseConcept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "concept_name", nullable = false)
    private String conceptName;

    @Enumerated(EnumType.ORDINAL)
    private RoleInExercise roleInExercise;
}
