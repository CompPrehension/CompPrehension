package com.example.demo.models.Dao;


import com.example.demo.models.entities.Concept;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ConceptDao extends CrudRepository<Concept, Long> {
    Optional<Concept> findConceptById(Long id);
}
