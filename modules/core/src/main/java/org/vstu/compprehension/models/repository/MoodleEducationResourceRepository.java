package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.educationresource.MoodleEducationResourceEntity;

import java.util.Optional;

@Repository
public interface MoodleEducationResourceRepository extends JpaRepository<MoodleEducationResourceEntity, Long> {
    Optional<MoodleEducationResourceEntity> findByUrl(String url);
}
