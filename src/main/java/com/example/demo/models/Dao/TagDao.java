package com.example.demo.models.Dao;

import com.example.demo.models.entities.Tag;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TagDao extends CrudRepository<Tag, Long> {
    Optional<Tag> findTagById(Long id);
}
