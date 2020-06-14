package com.example.demo.models.Dao;

import com.example.demo.models.entities.ExerciseDisplayingFeedbackType;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ExerciseDisplayingFeedbackTypeDao extends CrudRepository<ExerciseDisplayingFeedbackType, Long> {
    Optional<ExerciseDisplayingFeedbackType> findExerciseDisplayingFeedbackTypeById(Long id);
}
