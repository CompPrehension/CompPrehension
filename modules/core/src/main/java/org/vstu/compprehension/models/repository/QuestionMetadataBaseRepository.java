package org.vstu.compprehension.models.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionMetadataBaseEntity;

import java.util.Collection;
import java.util.List;

// Базовый пользовательский интерфейс
@NoRepositoryBean
public interface QuestionMetadataBaseRepository <T extends QuestionMetadataBaseEntity> extends CrudRepository<T, Integer> {

//    @Query("select * from #{#entityName} q where q._stage = 3", nativeQuery = true)  // not tested
    @Query("select q from #{#entityName} q where q.stage = 3")  // Note: db field `_stage` mapped by entity to `stage`
    List<QuestionMetadataBaseEntity> findAllReady();

//    @Query("select q from #{#entityName} q where q.stage = 3 and q.concept_bits IN :conceptBitEntries")  // Note: db field `_stage` mapped by entity to `stage`
//    List<QuestionMetadataBaseEntity> findAllWithConcepts(Collection<Integer> conceptBitEntries, Pageable pageable);

    @Query("select q from #{#entityName} q where q.stage = 3 AND q.conceptBits IN :values")  // Note: db field `concept_bits` mapped by entity to `conceptBits`
    List<QuestionMetadataBaseEntity> findAllWithConcepts(@Param("values") Collection<Integer> conceptBitEntries);

}