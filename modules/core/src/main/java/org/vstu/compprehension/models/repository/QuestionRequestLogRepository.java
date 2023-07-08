package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;

import java.util.List;

@Repository
public interface QuestionRequestLogRepository extends CrudRepository<QuestionRequestLogEntity, Long> {

    @Query("SELECT r FROM #{#entityName} r WHERE r.domainShortname = :domainShortName AND r.outdated = 0 AND r.foundCount <= :countThreshold")
    List<QuestionRequestLogEntity> findAllNotProcessed(
            @Param("domainShortName") String domainShortName,
            @Param("countThreshold") int countThreshold
    );


    /** Проверка на то, подходит ли вопрос под этот QR. Некоторые проверки опущены в предположении о том, что проверяемый вопрос является новым, то есть его ID ещё неизвестен.
     * @param meta метаданные Целевого вопроса
     * @param qr запрос на поиск вопросов для проверки
     * @return true, если вопрос подходит
     */
    static boolean doesQuestionSuitQR(@NotNull QuestionMetadataEntity meta, @NotNull QuestionRequestLogEntity qr) {

        // all checks required ti determine if the Q suits for the QR
        // see also: org.vstu.compprehension.models.repository.QuestionMetadataRepository#findSampleAroundComplexityWithoutQIds

        // Если не совпадает имя домена – мы пытаемся сделать что-то Неправильно!
        if (qr.getDomainShortname() != null && ! qr.getDomainShortname().equalsIgnoreCase(meta.getDomainShortname())
        ) {
            throw new RuntimeException(String.format("Trying matching a question with a QuestionRequest(LogEntity) of different domain ! (%s != %s)", meta.getDomainShortname(), qr.getDomainShortname()));
        }

        // проверка запрещаемых критериев
        if (meta.getSolutionSteps() < qr.getStepsMin()
            || meta.getSolutionSteps() > qr.getStepsMax()
            || (meta.getConceptBits() & qr.getConceptsDeniedBitmask()) != 0
            || (meta.getViolationBits() & qr.getLawsDeniedBitmask()) != 0
        ) {
            return false;
        }

        // Если есть запрет по ID шаблона или имени вопроса
        if (qr.getDeniedQuestionTemplateIds() != null && qr.getDeniedQuestionTemplateIds().contains(meta.getTemplateId())
            || qr.getDeniedQuestionNames() != null && qr.getDeniedQuestionNames().contains(meta.getName())
        ) {
            return false;
        }

        // сложность не проверяем (пусть будет всякая)

        // проверка на то, что присутствует хотя бы один целевой
        // В текущем варианте: вопрос подходит, если хотя бы по одному параметру есть совпадение.
        if ((meta.getTraceConceptBits() & qr.getTraceConceptsTargetedBitmask()) != 0
            || (meta.getConceptBits() & qr.getConceptsTargetedBitmask()) != 0
            || (meta.getLawBits() & qr.getLawsTargetedBitmask()) != 0
        ) {
            return true;
        }


        // проверка на отсутствие целевых
        // В текущем варианте: вопрос подходит, если никакие целевые не заданы и по запрещающим критериям (выше) он проходит.
        if (qr.getTraceConceptsTargetedBitmask() == 0
            && qr.getConceptsTargetedBitmask() == 0
            && qr.getLawsTargetedBitmask() == 0
        ) {
            return true;
        }

        /*
        // по всем таргетам есть совпадение (??)
        if (qr.getTraceConceptsTargetedBitmask() != 0 && (meta.getTraceConceptBits() & qr.getTraceConceptsTargetedBitmask()) == 0
            || qr.getConceptsTargetedBitmask() != 0 && (meta.getConceptBits() & qr.getConceptsTargetedBitmask()) == 0
            || qr.getLawsTargetedBitmask() != 0 && (meta.getLawBits() & qr.getLawsTargetedBitmask()) == 0
        ) {
            return true;
        }*/

        return false;
    }

}














