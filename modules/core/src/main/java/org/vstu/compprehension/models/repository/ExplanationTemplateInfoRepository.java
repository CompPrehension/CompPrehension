package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vstu.compprehension.models.entities.ExplanationTemplateInfoEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplanationTemplateInfoRepository extends JpaRepository<ExplanationTemplateInfoEntity, Long> {
}
