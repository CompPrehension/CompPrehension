package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.course.ExternalAccountEntity;
import org.vstu.compprehension.models.entities.course.ExternalAccountId;

@Repository
public interface ExternalAccountRepository extends JpaRepository<ExternalAccountEntity, ExternalAccountId> {
}
