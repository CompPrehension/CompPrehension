package com.example.demo.models.repository;

import com.example.demo.models.entities.QuestionConceptOrder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionConceptOrderRepository extends CrudRepository<QuestionConceptOrder, Long> {
}
