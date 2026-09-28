package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LtiRegistrationRepository extends JpaRepository<LtiRegistrationEntity, Long> {
    @EntityGraph(attributePaths = "educationResource")
    Optional<LtiRegistrationEntity> findByIssuerAndClientId(String issuer, String clientId);

    @EntityGraph(attributePaths = "educationResource")
    List<LtiRegistrationEntity> findAllByIssuer(String issuer);

    @EntityGraph(attributePaths = "educationResource")
    List<LtiRegistrationEntity> findAllByOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LtiRegistrationEntity r set r.description = :description where r.id = :id")
    int updateDescription(@Param("id") long id, @Param("description") String description);
}
