package com.example.demo.models.Dao;


import com.example.demo.models.entities.AnswerObject;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AnswerObjectDao extends CrudRepository<AnswerObject, Long> {
    Optional<AnswerObject> findAnswerObjectById(Long id);
}
