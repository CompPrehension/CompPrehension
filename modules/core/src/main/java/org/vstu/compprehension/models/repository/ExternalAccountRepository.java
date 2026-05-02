package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExternalAccountEntity;
import org.vstu.compprehension.models.entities.course.ExternalAccountId;

import java.util.Optional;

@Repository
public interface ExternalAccountRepository extends JpaRepository<ExternalAccountEntity, ExternalAccountId> {
    Optional<ExternalAccountEntity> findByUser_IdAndEducationResource_Id(Long userId, Long educationResourceId);
}
