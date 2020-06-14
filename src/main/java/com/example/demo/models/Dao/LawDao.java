package com.example.demo.models.Dao;

import com.example.demo.models.entities.Law;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LawDao extends CrudRepository<Law, Long> {
    Optional<Law> findLawById(Long id);
}
