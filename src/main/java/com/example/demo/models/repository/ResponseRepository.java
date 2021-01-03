package com.example.demo.models.repository;

import com.example.demo.models.entities.ResponseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends CrudRepository<ResponseEntity, Long> {
}
