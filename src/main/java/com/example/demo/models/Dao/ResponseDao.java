package com.example.demo.models.Dao;

import com.example.demo.models.entities.Response;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ResponseDao extends CrudRepository<Response, Long> {
    Optional<Response> findResponseById(Long id);
}
