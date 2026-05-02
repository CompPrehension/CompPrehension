package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.CourseEntity;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, Long> {
    Optional<CourseEntity> findByExternalCourseIdAndEducationResource_Id(String externalCourseId, Long educationResourceId);
}
