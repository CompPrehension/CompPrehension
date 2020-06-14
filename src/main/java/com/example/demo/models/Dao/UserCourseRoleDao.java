package com.example.demo.models.Dao;

import com.example.demo.models.entities.UserCourseRole;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserCourseRoleDao extends CrudRepository<UserCourseRole, Long> {
    Optional<UserCourseRole> findUserCourseRoleById(Long id);
}
