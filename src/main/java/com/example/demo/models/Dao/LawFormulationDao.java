package com.example.demo.models.Dao;

import com.example.demo.models.entities.LawFormulation;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LawFormulationDao extends CrudRepository<LawFormulation, Long> {
    Optional<LawFormulation> findLawFormulationById(Long id);
}
