package com.example.demo.models.repository;


import com.example.demo.models.entities.Domain;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DomainRepository extends CrudRepository<Domain, String> {
}
