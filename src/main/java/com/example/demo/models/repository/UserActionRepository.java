package com.example.demo.models.repository;

import com.example.demo.models.entities.UserAction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActionRepository extends CrudRepository<UserAction, Long> {
}
