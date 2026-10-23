package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.enums.EducationResourceTrustStatus;
import org.vstu.compprehension.entities.external_system.EducationResourceEntity;

import java.util.Optional;

@Repository
public interface EducationResourceRepository extends JpaRepository<EducationResourceEntity, Long> {
    Optional<EducationResourceEntity> findByUrl(String url);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO education_resource (url, trust_status)
            VALUES (:url, :trustStatus)
            """, nativeQuery = true)
    int createIfAbsent(@Param("url") String url, @Param("trustStatus") String trustStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update EducationResourceEntity r set r.trustStatus = :trustStatus where r.id = :id")
    int updateTrustStatus(@Param("id") long id, @Param("trustStatus") EducationResourceTrustStatus trustStatus);
}
