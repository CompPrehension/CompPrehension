package org.vstu.compprehension.models.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;

import java.util.List;

@Repository
public interface QuestionRequestLogRepository extends CrudRepository<QuestionRequestLogEntity, Long> {

    @Query("SELECT r FROM #{#entityName} r WHERE r.domainShortname = :domainShortName AND r.outdated = 0 AND r.foundCount <= :countThreshold")
    List<QuestionRequestLogEntity> findAllNotProcessed(
            @Param("domainShortName") String domainShortName,
            @Param("countThreshold") int countThreshold
    );


    public static boolean doesQuestionSuitQR(Question question, QuestionRequestLogEntity qr) {
        // TODO

        return false;
    }

}














