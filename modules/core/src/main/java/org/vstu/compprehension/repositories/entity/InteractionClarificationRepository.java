package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.InteractionClarificationEntity;

import java.util.Optional;

@Repository
public interface InteractionClarificationRepository extends JpaRepository<InteractionClarificationEntity, Long> {
    Optional<InteractionClarificationEntity> findByInteractionId(long interactionId);
}
