package com.example.demo.models.repository;

import com.example.demo.models.entities.Mistake;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MistakeRepository extends CrudRepository<Mistake, Long> {
}
