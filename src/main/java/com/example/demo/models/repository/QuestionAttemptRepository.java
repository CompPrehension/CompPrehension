package com.example.demo.models.repository;

import com.example.demo.models.entities.QuestionAttemptEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionAttemptRepository extends CrudRepository<QuestionAttemptEntity, Long> {
}
