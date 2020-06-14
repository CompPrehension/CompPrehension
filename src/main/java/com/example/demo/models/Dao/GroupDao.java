package com.example.demo.models.Dao;

import com.example.demo.models.entities.Group;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface GroupDao extends CrudRepository<Group, Long> {
    Optional<Group> findGroupById(Long id);
}
