package com.example.demo.models.Dao;

import com.example.demo.models.entities.ExerciseAttempt;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExerciseAttemptDao extends CrudRepository<ExerciseAttempt, Long> {
    Optional<ExerciseAttempt> findExerciseAttemptById(Long id);
}
