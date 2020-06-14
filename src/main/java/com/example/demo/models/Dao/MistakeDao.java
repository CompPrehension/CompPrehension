package com.example.demo.models.Dao;

import com.example.demo.models.entities.Mistake;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface MistakeDao extends CrudRepository<Mistake, Long> {
    Optional<Mistake> findMistakeById(Long id);
}
