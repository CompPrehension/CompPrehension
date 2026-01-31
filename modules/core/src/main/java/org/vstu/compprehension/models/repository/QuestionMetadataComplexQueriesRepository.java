package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface QuestionMetadataComplexQueriesRepository {
    int countQuestions(QuestionBankSearchRequest qr);

    int countTopRatedQuestions(QuestionBankSearchRequest qr);

    List<Integer> findMostUsedMetadataIds(@Nullable Integer weekUsageThreshold, @Nullable Integer dayUsageThreshold, @Nullable Integer hourUsageThreshold, @Nullable Integer min15UsageThreshold, @Nullable Integer min5UsageThreshold);

    /**
     * Найти самые лучше неиспользованные вопросы.
     * (По количеству найденных вопросов также определяется потребность в генерации новых, — когда их становится слишком мало.)
     * @param qr поисковый запрос к банку вопросов
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findTopRatedUnusedMetadata(QuestionBankSearchRequest qr, int limitNumber);

    /**
     * Найти самые лучшие вопросы.
     * @param qr поисковый запрос к банку вопросов
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findTopRatedMetadata(QuestionBankSearchRequest qr, int limitNumber);

    /**
     * Найти хорошие, потенциально использованные вопросы.
     * Этот метод поиска больше акцентируется на совпадении по сложности.
     * @param qr поисковый запрос к банку вопросов
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findMetadata(QuestionBankSearchRequest qr, int limitNumber);

    /**
     * Найти любые минимально подходящие вопросы.
     * Этот метод поиска требует только попадания величины solution_steps в фиксированный диапазон,
     * остальные критерии могут быть удовлетворены не полностью и используются для сортировки кандидатов по убыванию.
     * @param qr поисковый запрос к банку вопросов
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findMetadataRelaxed(QuestionBankSearchRequest qr, int limitNumber);
    
    @Modifying
    int deleteMetadataFromDate(LocalDate date);

    Integer clone(
            Integer sourceId,
            String newName,
            String newTemplateId,
            Date created,
            Integer generationRequestId
    );
}
