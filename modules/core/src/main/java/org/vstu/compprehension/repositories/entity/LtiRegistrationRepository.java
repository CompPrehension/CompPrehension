package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LtiRegistrationRepository extends JpaRepository<LtiRegistrationEntity, Long> {
    @EntityGraph(attributePaths = "educationResource")
    Optional<LtiRegistrationEntity> findByIssuer(String issuer);

    @EntityGraph(attributePaths = "educationResource")
    List<LtiRegistrationEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByEducationResourceId(long educationResourceId);
}
