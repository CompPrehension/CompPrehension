package org.vstu.compprehension.businesslogic.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.frontend.dto.QuestionBankSearchStatsDto;
import org.vstu.compprehension.businesslogic.QuestionRequest;
import org.vstu.compprehension.data.questionbank.GenerationRequestGroupData;
import org.vstu.compprehension.data.questionbank.NewBankQuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.data.question.QuestionRequestLogData;
import org.vstu.compprehension.businesslogic.QuestionBankSearchRequest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Банк готовых заданий: поиск подходящего вопроса и заказ генерации, когда их мало.
 */
public interface QuestionBank {
    boolean isMatch(@NotNull QuestionMetadataData meta, @NotNull QuestionRequestLogData qrLog);

    boolean isMatch(@NotNull QuestionMetadataData meta, @NotNull QuestionBankSearchRequest qr);

    int countQuestions(QuestionRequest qr);

    QuestionBankSearchStatsDto getStatsByQuestionRequest(QuestionRequest qr, int limit);

    QuestionBankSearchResult searchQuestions(@NotNull QuestionRequest qr, int limit, int generatorThreshold, int generatorAdditionalQuestionsToGenerate);

    @Nullable QuestionMetadataWithData loadQuestion(int questionMetadataId);

    boolean questionExists(String questionName);

    long countQuestionsInDomain(String domainShortname);

    Set<String> findExistingNames(String domainShortname, Collection<String> questionNames);

    Set<String> findExistingTemplateIds(String domainShortname, Collection<String> templateIds);

    Set<String> findProcessedOrigins(String domainShortname, LocalDateTime since);

    int saveQuestions(List<NewBankQuestionData> questions);

    void replaceQuestionBody(int metadataId, SerializableQuestion body);

    List<GenerationRequestGroupData> findActualGenerationRequests(String domainShortname, LocalDateTime createdAfter);

    void refreshGenerationRequests(Integer[] generationRequestIds);

    Optional<Long> findLastGenerationRequestIdOfAttempt(long exerciseAttemptId);
}
