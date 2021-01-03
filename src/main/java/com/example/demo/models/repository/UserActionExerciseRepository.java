package com.example.demo.models.repository;

import com.example.demo.models.entities.UserActionExerciseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActionExerciseRepository extends CrudRepository<UserActionExerciseEntity, Long> {
}
