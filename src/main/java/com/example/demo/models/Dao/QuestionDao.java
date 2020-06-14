package com.example.demo.models.Dao;

import com.example.demo.models.entities.Question;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface QuestionDao extends CrudRepository<Question, Long> {
    Optional<Question> findQuestionById(Long id);
}
