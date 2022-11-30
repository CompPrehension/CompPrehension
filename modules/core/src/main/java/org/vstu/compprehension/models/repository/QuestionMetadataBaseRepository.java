package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.vstu.compprehension.models.entities.QuestionMetadataBaseEntity;

import java.util.List;

// Базовый пользовательский интерфейс
@NoRepositoryBean
public interface QuestionMetadataBaseRepository <T extends QuestionMetadataBaseEntity> extends CrudRepository<T, Integer> {

    @Query("select t from #{#entityName} t where t.deleted = ?1")
    List<QuestionMetadataBaseEntity> findMarked(Boolean deleted);
}