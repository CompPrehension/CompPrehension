package com.example.demo.models.repository;

import com.example.demo.models.entities.QuestionAttempt;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionAttemptRepository extends CrudRepository<QuestionAttempt, Long> {
}
