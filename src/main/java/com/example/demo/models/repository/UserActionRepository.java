package com.example.demo.models.repository;

import com.example.demo.models.entities.UserActionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActionRepository extends CrudRepository<UserActionEntity, Long> {
}
