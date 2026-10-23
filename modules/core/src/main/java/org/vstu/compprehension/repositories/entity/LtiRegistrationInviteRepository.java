package org.vstu.compprehension.repositories.entity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.external_system.LtiRegistrationEntity;
import org.vstu.compprehension.entities.external_system.LtiRegistrationInviteEntity;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LtiRegistrationInviteRepository extends JpaRepository<LtiRegistrationInviteEntity, Long> {
    Optional<LtiRegistrationInviteEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LtiRegistrationInviteEntity> findLockedByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LtiRegistrationInviteEntity i set i.registration = null where i.registration.id = :registrationId")
    int detachFromRegistration(@Param("registrationId") long registrationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LtiRegistrationInviteEntity i set i.usedAt = :usedAt, i.registration = :registration where i.id = :id")
    int markUsed(@Param("id") long id, @Param("registration") LtiRegistrationEntity registration,
                 @Param("usedAt") Instant usedAt);
}
