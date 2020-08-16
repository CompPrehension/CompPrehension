package com.example.demo.models.repository;

import com.example.demo.models.entities.ExerciseDisplayingFeedbackType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseDisplayingFeedbackTypeRepository extends CrudRepository<ExerciseDisplayingFeedbackType, Long> {
}
