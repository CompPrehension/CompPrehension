package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.UserCourseRoleEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCourseRoleRepository extends CrudRepository<UserCourseRoleEntity, Long> {
}
