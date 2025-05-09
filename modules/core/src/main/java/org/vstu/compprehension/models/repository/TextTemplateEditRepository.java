package org.vstu.compprehension.models.repository;

import org.springframework.data.repository.CrudRepository;
import org.vstu.compprehension.models.entities.TextTemplateEditEntity;

import java.util.List;


public interface TextTemplateEditRepository extends CrudRepository<TextTemplateEditEntity, TextTemplateEditEntity.TextTemplateEditKey> {
    List<TextTemplateEditEntity> findAllByKey_DomainName(String domainId);
}
