package com.example.demo.models.repository;

import com.example.demo.models.entities.LawFormulation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LawFormulationRepository extends CrudRepository<LawFormulation, Long> {
}
