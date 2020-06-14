package com.example.demo.models.Dao;

import com.example.demo.models.entities.ExerciseLaws;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExerciseLawsDao extends CrudRepository<ExerciseLaws, Long> {
    Optional<ExerciseLaws> findExerciseLawsById(Long id);
}
