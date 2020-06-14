package com.example.demo.models.Dao;

import com.example.demo.models.entities.UserActionExercise;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserActionExerciseDao extends CrudRepository<UserActionExercise, Long> {
    Optional<UserActionExercise> findUserActionExerciseById(Long id);
}
