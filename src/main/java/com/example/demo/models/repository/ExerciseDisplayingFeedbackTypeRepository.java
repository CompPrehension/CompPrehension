package com.example.demo.models.repository;

import com.example.demo.models.entities.ExerciseDisplayingFeedbackTypeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseDisplayingFeedbackTypeRepository extends CrudRepository<ExerciseDisplayingFeedbackTypeEntity, Long> {
}
