package com.example.demo.models.Dao;

import com.example.demo.models.entities.QuestionConceptOrder;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface QuestionConceptOrderDao extends CrudRepository<QuestionConceptOrder, Long> {
    Optional<QuestionConceptOrder> findQuestionConceptOrderById(Long id);
}
