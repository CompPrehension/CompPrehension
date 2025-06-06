package org.vstu.compprehension.models.repository;

import jakarta.persistence.QueryHint;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.dto.ComplexityStats;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

// Основной интерфейс для поиска вопросов по их метаданным
@Primary
@Repository
public interface QuestionMetadataRepository extends CrudRepository<QuestionMetadataEntity, Integer>, QuestionMetadataComplexQueriesRepository {

    @NotNull
    @Query(value = 
            "select m from QuestionMetadataEntity m " +
            "where m.id > :lastLoadedId " +
            "order by m.id " +
            "limit :limit")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<QuestionMetadataEntity> loadPage(@Param("lastLoadedId") int lastLoadedId, @Param("limit") int limit);

    @NotNull
    @Query(value =
            "select m from QuestionMetadataEntity m " +
            "inner join fetch m.questionData " +
            "where m.id > :lastLoadedId " +
            "order by m.id " +
            "limit :limit"
    )
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<QuestionMetadataEntity> loadPageWithData(@Param("lastLoadedId") int lastLoadedId, @Param("limit") int limit);

    @NotNull
    @Query(value =
            "select m from QuestionMetadataEntity m " +
            "where m.id > :lastLoadedId AND m.domainShortname = :domainShortName " +
            "order by m.id " +
            "limit :limit"
    )
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<QuestionMetadataEntity> loadPage(@Param("lastLoadedId") int lastLoadedId, @Param("domainShortName") String domainShortName, @Param("limit") int limit);
    
    @Query
    long countByDomainShortname(String domainShortname);
    
    @NotNull
    @Override
    Iterable<QuestionMetadataEntity> findAll();

    @NotNull
    @Query("select q from #{#entityName} q where q.name = :questionName")
    List<QuestionMetadataEntity> findByName(@Param("questionName") String questionName);

    @Query("select meta from QuestionEntity q " +
            "join q.metadata meta " +
            "where q.exerciseAttempt.id = :attemptId AND meta is not null " +
            "order by q.createdAt desc " +
            "limit :limit")
    List<QuestionMetadataEntity> findLastNExerciseAttemptMeta(@Param("attemptId") long attemptId, @Param("limit") int limit);

    @Query
    boolean existsByName(String questionName);

    @Query("select distinct m.name from QuestionMetadataEntity m where m.domainShortname = :domainShortname and m.name in :questionNames")
    HashSet<String> findExistingNames(@Param("domainShortname") String domainShortname, @Param("questionNames") Collection<String> questionNames);

    @Query("select distinct m.templateId from QuestionMetadataEntity m where m.domainShortname = :domainShortname and m.templateId in :templateIds")
    HashSet<String> findExistingTemplateIds(@Param("domainShortname") String domainShortname, @Param("templateIds") Collection<String> templateIds);

    @Query(value = "select new org.vstu.compprehension.dto.ComplexityStats(" +
            "count(*), " +
            "min(q.integralComplexity), " +
            "avg(q.integralComplexity), " +
            "max(q.integralComplexity)) " +
            "from QuestionMetadataEntity q where q.domainShortname = :DOMAIN_NAME ")
    ComplexityStats getStatOnComplexityField(
            @Param("DOMAIN_NAME") String domainShortName
    );

    @NotNull
    @Query("select distinct(q.origin) from QuestionMetadataEntity q where q.domainShortname = :domainShortname and q.generatedBy is null")
    HashSet<String> findFullyProcessedOrigins(@Param("domainShortname") String domainShortname);

    @NotNull
    @Query("select distinct(q.origin) from QuestionMetadataEntity q where q.domainShortname = :domainShortname")
    HashSet<String> findProcessedOrigins(@Param("domainShortname") String domainShortname);

    @NotNull
    @Query("select distinct(q.origin) from QuestionMetadataEntity q where q.domainShortname = :domainShortname and q.createdAt >= :dateFrom")
    HashSet<String> findProcessedOrigins(@Param("domainShortname") String domainShortname, @Param("dateFrom") LocalDateTime dateFrom);

    @Query("select exists(select m.id from QuestionMetadataEntity m where m.domainShortname = :domainShortname and m.templateId = :templateId)")
    boolean templateExists(@Param("domainShortname") String domainShortname, @Param("templateId") String templateId);

    @NotNull
    @Query("select distinct(m.templateId) from QuestionMetadataEntity m where m.domainShortname = :domainShortname and m.templateId is not null")
    HashSet<String> findAllTemplates(@Param("domainShortname") String domainShortname);
}
