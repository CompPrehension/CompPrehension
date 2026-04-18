package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.vstu.compprehension.models.entities.externalaccount.ExternalAccountEntity;

@NoRepositoryBean
public interface ExternalAccountRepository extends JpaRepository<ExternalAccountEntity, Long> {
}
