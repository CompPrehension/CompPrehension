package com.example.demo.models.repository;

import com.example.demo.models.entities.InteractionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends CrudRepository<InteractionEntity, Long> {
}
