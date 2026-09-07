package org.vstu.compprehension.repositories.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vstu.compprehension.entities.ExplanationTemplateInfoEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplanationTemplateInfoRepository extends JpaRepository<ExplanationTemplateInfoEntity, Long> {
}
