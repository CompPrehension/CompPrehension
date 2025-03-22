package org.vstu.compprehension.models.repository;

import org.vstu.compprehension.models.entities.ExplanationTemplateInfoEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplanationTemplateInfoRepository extends CrudRepository<ExplanationTemplateInfoEntity, Long> {
}
