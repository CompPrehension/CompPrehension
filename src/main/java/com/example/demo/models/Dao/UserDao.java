package com.example.demo.models.Dao;

import com.example.demo.models.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserDao extends CrudRepository<User, Long> {
    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByLogin(String login);

    Optional<User> findUserById(Long id);
}