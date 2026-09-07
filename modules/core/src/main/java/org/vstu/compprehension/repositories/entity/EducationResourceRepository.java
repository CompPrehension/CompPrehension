package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.enums.EducationResourceType;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationResourceRepository extends JpaRepository<EducationResourceEntity, Long> {
    Optional<EducationResourceEntity> findByUrlAndType(String url, EducationResourceType type);

    List<EducationResourceEntity> findByTypeAndTrustStatus(EducationResourceType type, EducationResourceTrustStatus trustStatus);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO education_resource (url, type)
            VALUES (:url, :type)
            """, nativeQuery = true)
    int createIfAbsent(@Param("url") String url, @Param("type") String type);
}
