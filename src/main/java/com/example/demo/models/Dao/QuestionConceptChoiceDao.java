package com.example.demo.models.Dao;

import com.example.demo.models.entities.QuestionConceptChoice;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface QuestionConceptChoiceDao extends CrudRepository<QuestionConceptChoice, Long> {
    Optional<QuestionConceptChoice> findQuestionConceptChoiceById(Long id);

}
