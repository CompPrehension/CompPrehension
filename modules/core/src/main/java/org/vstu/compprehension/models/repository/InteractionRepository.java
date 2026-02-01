package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface InteractionRepository extends CrudRepository<InteractionEntity, Long> {
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO interaction
            (order_number, last_supplementary_question, interaction_type, feedback_id, question_id, created_at)
            VALUES
            (0, NULL, :interactionType, NULL, :questionId, :createdAt)
         """, nativeQuery = true)
    void createFakeInteraction(@Param("interactionType") String interactionType, @Param("questionId") Long questionId, @Param("createdAt") Instant createdAt);
}
