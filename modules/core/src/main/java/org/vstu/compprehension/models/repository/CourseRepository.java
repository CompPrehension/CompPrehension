package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.CourseEntity;

@Repository
public interface CourseRepository extends CrudRepository<CourseEntity, Long> {
}
