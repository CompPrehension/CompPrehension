package com.example.demo.models.repository;


import com.example.demo.models.entities.Backend;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackendRepository extends CrudRepository<Backend, Long> {
}
