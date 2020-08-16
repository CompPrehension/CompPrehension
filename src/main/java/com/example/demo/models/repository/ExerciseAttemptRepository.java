package com.example.demo.models.repository;

import com.example.demo.models.entities.ExerciseAttempt;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseAttemptRepository extends CrudRepository<ExerciseAttempt, Long> {
}
