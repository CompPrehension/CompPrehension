package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.entities.ViolationEntity;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<ViolationEntity, Long> {
    @Query("""
            select distinct ve from ViolationEntity ve
            left join fetch ve.explanationTemplateInfo
            where ve.interaction.question.id = :questionId
            """)
    List<ViolationEntity> findAllByQuestionIdFetchingTemplates(@Param("questionId") long questionId);
}
