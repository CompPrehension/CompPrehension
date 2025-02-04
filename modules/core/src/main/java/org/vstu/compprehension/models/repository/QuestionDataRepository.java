package org.vstu.compprehension.models.repository;

import jakarta.persistence.QueryHint;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.entities.QuestionDataEntity;

import java.util.List;
import java.util.Optional;

public interface QuestionDataRepository extends CrudRepository<QuestionDataEntity, Integer> {
    @Query("select q from QuestionDataEntity q inner join QuestionMetadataEntity m on q.id = m.questionData.id where m.id = ?1")
    Optional<QuestionDataEntity> findByMetadataId(int questionMetadataId);

    @NotNull
    @Query(value =
            "select m from QuestionDataEntity m " +
                    "where m.id > :lastLoadedId " +
                    "order by m.id " +
                    "limit :limit"
    )
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<QuestionDataEntity> loadPage(@Param("lastLoadedId") int lastLoadedId, @Param("limit") int limit);
}
