package com.example.demo.models.repository;

import com.example.demo.models.entities.ExerciseQuestionType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseQuestionTypeRepository extends CrudRepository<ExerciseQuestionType, Long> {
}
