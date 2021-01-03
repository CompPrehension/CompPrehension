package com.example.demo.models.repository;

import com.example.demo.models.entities.UserCourseRoleEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCourseRoleRepository extends CrudRepository<UserCourseRoleEntity, Long> {
}
