package com.example.demo.models.repository;

import com.example.demo.models.entities.QuestionConceptChoice;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionConceptChoiceRepository extends CrudRepository<QuestionConceptChoice, Long> {

}
