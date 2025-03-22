package org.vstu.compprehension.models.repository;

import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;

import java.util.List;

public interface QuestionMetadataComplexQueriesRepository {
    int countQuestions(QuestionBankSearchRequest qr, float complexityWindow);

    int countTopRatedQuestions(QuestionBankSearchRequest qr, float complexityWindow);

    List<Integer> findMostUsedMetadataIds(@Nullable Integer weekUsageThreshold, @Nullable Integer dayUsageThreshold, @Nullable Integer hourUsageThreshold, @Nullable Integer min15UsageThreshold, @Nullable Integer min5UsageThreshold);

    /**
     * Найти самые лучше неиспользованные вопросы.
     * (По количеству найденных вопросов также определяется потребность в генерации новых, — когда их становится слишком мало.)
     * @param qr поисковый запрос к банку вопросов
     * @param complexityWindow ширина допуска для сопоставления complexity
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findTopRatedUnusedMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber);

    /**
     * Найти самые лучшие вопросы.
     * @param qr поисковый запрос к банку вопросов
     * @param complexityWindow ширина допуска для сопоставления complexity
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findTopRatedMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber);

    /**
     * Найти хорошие, потенциально использованные вопросы.
     * Этот метод поиска больше акцентируется на совпадении по сложности.
     * @param qr поисковый запрос к банку вопросов
     * @param complexityWindow ширина допуска для сопоставления complexity
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findMetadata(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber);

    /**
     * Найти любые минимально подходящие вопросы.
     * Этот метод поиска требует только попадания величины solution_steps в фиксированный диапазон,
     * остальные критерии могут быть удовлетворены не полностью и используются для сортировки кандидатов по убыванию.
     * @param qr поисковый запрос к банку вопросов
     * @param complexityWindow ширина допуска для сопоставления complexity
     * @param limitNumber максимальное число вопросов в результате
     * @return подходящие вопросы
     */
    List<QuestionMetadataEntity> findMetadataRelaxed(QuestionBankSearchRequest qr, float complexityWindow, int limitNumber);
}
