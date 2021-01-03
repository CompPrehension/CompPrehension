package com.example.demo.models.repository;


import com.example.demo.models.entities.AnswerObjectEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerObjectRepository extends CrudRepository<AnswerObjectEntity, Long> {
}
