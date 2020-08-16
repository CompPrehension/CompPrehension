package com.example.demo.models.repository;

import com.example.demo.models.entities.ExerciseConcept;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseConceptRepository extends CrudRepository<ExerciseConcept, Long> {
}
