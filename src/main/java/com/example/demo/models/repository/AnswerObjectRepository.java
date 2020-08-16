package com.example.demo.models.repository;


import com.example.demo.models.entities.AnswerObject;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerObjectRepository extends CrudRepository<AnswerObject, Long> {
}
