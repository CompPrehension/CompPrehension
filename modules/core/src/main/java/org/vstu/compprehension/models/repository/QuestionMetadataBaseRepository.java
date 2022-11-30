package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.vstu.compprehension.models.entities.QuestionMetadataBaseEntity;

import java.util.List;

// Базовый пользовательский интерфейс
@NoRepositoryBean
public interface QuestionMetadataBaseRepository <T extends QuestionMetadataBaseEntity> extends CrudRepository<T, Integer> {

//    @Query("select * from #{#entityName} q where q._stage = 3", nativeQuery = true)  // not tested
    @Query("select q from #{#entityName} q where q.stage = 3")  // Note: db field `_stage` mapped by entity to `stage`
    List<QuestionMetadataBaseEntity> findAllReady();
}