package com.example.demo.models.repository;

import com.example.demo.models.entities.ExerciseLaws;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseLawsRepository extends CrudRepository<ExerciseLaws, Long> {
}
