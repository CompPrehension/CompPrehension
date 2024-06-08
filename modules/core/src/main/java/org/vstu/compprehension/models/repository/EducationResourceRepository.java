package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.EducationResourceEntity;

import java.util.Optional;

@Repository
public interface EducationResourceRepository  extends CrudRepository<EducationResourceEntity, Long> {

    Optional<EducationResourceEntity> findByName(String name);

    Optional<EducationResourceEntity> findByUrl(String url);
}
