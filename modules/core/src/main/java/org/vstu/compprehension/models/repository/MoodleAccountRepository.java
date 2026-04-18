package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.externalaccount.MoodleAccountEntity;

import java.util.Optional;

@Repository
public interface MoodleAccountRepository extends JpaRepository<MoodleAccountEntity, Long> {
    Optional<MoodleAccountEntity> findByUserIdAndEducationResourceId(Long userId, Long educationResourceId);

}
