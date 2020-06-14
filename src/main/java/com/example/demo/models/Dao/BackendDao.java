package com.example.demo.models.Dao;

import com.example.demo.models.businesslogic.backend.Backend;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BackendDao extends CrudRepository<Backend, Long> {
    Optional<Backend> findBackendById(Long id);
}
