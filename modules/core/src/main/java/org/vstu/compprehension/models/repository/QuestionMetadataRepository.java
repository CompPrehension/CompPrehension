package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.dto.ComplexityStats;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;

// Основной интерфейс для поиска вопросов по их метаданным
@Primary
@Repository
public interface QuestionMetadataRepository extends CrudRepository<QuestionMetadataEntity, Integer>, QuestionMetadataComplexQueriesRepository {

    @NotNull
    @Override
    Iterable<QuestionMetadataEntity> findAll();

    @NotNull
    @Query("select q from #{#entityName} q where q.name = :questionName")
    List<QuestionMetadataEntity> findByName(@Param("questionName") String questionName);


    @Query(value = "select new org.vstu.compprehension.dto.ComplexityStats(" +
            "count(*), " +
            "min(q.integralComplexity), " +
            "avg(q.integralComplexity), " +
            "max(q.integralComplexity)) " +
            "from QuestionMetadataEntity q where q.domainShortname = :DOMAIN_NAME AND q.stage = 3 " +
            "AND q.isDraft = false")
    ComplexityStats getStatOnComplexityField(
            @Param("DOMAIN_NAME") String domainShortName
    );

    @NotNull
    @Query("select distinct(q.origin) from #{#entityName} q where q.domainShortname = :domainName")  // ( AND q.stage = :stage ) ?
    List<String> findAllOrigins(
            @Param("domainName") String domainName
    );
}
