package com.example.demo.models.Dao;

import com.example.demo.models.entities.ExerciseQuestionType;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExerciseQuestionTypeDao extends CrudRepository<ExerciseQuestionType, Long> {
    Optional<ExerciseQuestionType> findExerciseQuestionTypeById(Long id);
}
