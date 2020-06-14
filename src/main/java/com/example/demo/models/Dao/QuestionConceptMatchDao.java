package com.example.demo.models.Dao;

import com.example.demo.models.entities.QuestionConceptMatch;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface QuestionConceptMatchDao extends CrudRepository<QuestionConceptMatch, Long> {
    Optional<QuestionConceptMatch> findQuestionConceptMatchById(Long id);
}
