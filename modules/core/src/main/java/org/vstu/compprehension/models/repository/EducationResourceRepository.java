package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;

import java.util.Optional;

@Repository
public interface EducationResourceRepository extends JpaRepository<EducationResourceEntity, Long> {
    Optional<EducationResourceEntity> findByUrlAndType(String url, EducationResourceType type);
}
