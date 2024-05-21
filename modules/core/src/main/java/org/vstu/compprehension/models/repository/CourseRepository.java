package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.CourseEntity;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;

import java.util.Optional;

@Repository
public interface CourseRepository extends CrudRepository<CourseEntity, Long> {

    Optional<CourseEntity> findByName(String name);

    Optional<CourseEntity> findByNameAndEducationResources(String name, EducationResourceEntity educationResources);
}
