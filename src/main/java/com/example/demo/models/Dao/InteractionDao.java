package com.example.demo.models.Dao;

import com.example.demo.models.entities.Interaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface InteractionDao extends CrudRepository<Interaction, Long> {
    Optional<Interaction> findInteractionById(Long id);
}
