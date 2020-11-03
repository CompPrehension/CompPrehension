package com.example.demo.models.repository;

import com.example.demo.models.entities.Law;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LawRepository extends CrudRepository<Law, String> {
}
