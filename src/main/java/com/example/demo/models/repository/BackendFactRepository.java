package com.example.demo.models.repository;

import com.example.demo.models.entities.BackendFactEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackendFactRepository extends CrudRepository<BackendFactEntity, Long> {
}
