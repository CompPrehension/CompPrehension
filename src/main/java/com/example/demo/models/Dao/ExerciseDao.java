package com.example.demo.models.Dao;

import com.example.demo.models.entities.Exercise;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExerciseDao extends CrudRepository<Exercise, Long> {
    Optional<Exercise> findExerciseById(Long id);
}
