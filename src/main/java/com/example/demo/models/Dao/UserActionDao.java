package com.example.demo.models.Dao;

import com.example.demo.models.entities.UserAction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserActionDao extends CrudRepository<UserAction, Long> {
    Optional<UserAction> findUserActionById(Long id);
}
