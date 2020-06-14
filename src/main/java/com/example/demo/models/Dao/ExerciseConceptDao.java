package com.example.demo.models.Dao;

import com.example.demo.models.entities.ExerciseConcept;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExerciseConceptDao extends CrudRepository<ExerciseConcept, Long> {
    Optional<ExerciseConcept> findExerciseConceptById(Long id);
}
