package com.example.demo.models.Dao;

import com.example.demo.models.entities.QuestionAttempt;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface QuestionAttemptDao extends CrudRepository<QuestionAttempt, Long> {
    Optional<QuestionAttempt> findQuestionAttemptById(Long id);
}
