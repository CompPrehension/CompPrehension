package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.entities.QuestionMetadataSearchRequestEntity;

@Repository
public interface QuestionMetadataSearchRequestRepository extends JpaRepository<QuestionMetadataSearchRequestEntity, Long> {
}
