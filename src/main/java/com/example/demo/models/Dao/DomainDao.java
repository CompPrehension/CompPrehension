package com.example.demo.models.Dao;


import com.example.demo.models.entities.Domain;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface DomainDao extends CrudRepository<Domain, Long> {
    Optional<Domain> findDomainById(Long id);
}
