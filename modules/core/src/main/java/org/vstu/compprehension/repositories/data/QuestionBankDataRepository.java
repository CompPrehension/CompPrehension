package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.data.questionbank.ComplexityStatsData;
import org.vstu.compprehension.data.questionbank.GenerationRequestGroupData;
import org.vstu.compprehension.data.questionbank.NewBankQuestionData;
import org.vstu.compprehension.data.question.QuestionMaskData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.questionbank.SearchIterationData;
import org.vstu.compprehension.data.questionbank.SearchQuality;
import org.vstu.compprehension.entities.QuestionDataEntity;
import org.vstu.compprehension.entities.QuestionGenerationRequestEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.entities.QuestionMetadataSearchRequestEntity;
import org.vstu.compprehension.repositories.entity.QuestionGenerationRequestRepository;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository.QuestionMaskView;
import org.vstu.compprehension.repositories.entity.QuestionMetadataSearchRequestRepository;
import org.vstu.compprehension.repositories.entity.SerializedQuestionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Банк заданий: метаданные вопросов, их тела, заявки на генерацию и журнал поиска.
 * <p>
 * Четыре таблицы в одном месте, потому что поиск вопроса — это одна операция, которая
 * их все и трогает: найти метаданные, поднять к ним тела, заказать генерацию, если
 * найденного мало, и записать, чем поиск кончился.
 * <p>
 * Тела вопросов поднимаются тем же числом запросов, что и метаданные: раньше связь
 * оставалась ленивой, и обход результата поиска стоил по запросу на каждую строку.
 */
@Repository
@RequiredArgsConstructor
public class QuestionBankDataRepository {

    private final QuestionMetadataRepository metadataRepository;
    private final SerializedQuestionRepository serializedQuestionRepository;
    private final QuestionGenerationRequestRepository generationRequestRepository;
    private final QuestionMetadataSearchRequestRepository searchRequestLogRepository;

    // ---------------------------------------------------------------- поиск

    /** Разброс сложности вопросов домена: по нему нормализуется сложность из запроса. */
    @Transactional(readOnly = true)
    public @NotNull ComplexityStatsData getComplexityStats(@NotNull String domainShortname) {
        var stats = metadataRepository.getStatOnComplexityField(domainShortname);
        return new ComplexityStatsData(
                stats.getCount() == null ? 0L : stats.getCount(),
                stats.getMin(), stats.getMean(), stats.getMax());
    }

    @Transactional(readOnly = true)
    public int countQuestions(@NotNull QuestionBankSearchRequest request) {
        return metadataRepository.countQuestions(request);
    }

    @Transactional(readOnly = true)
    public int countTopRatedQuestions(@NotNull QuestionBankSearchRequest request) {
        return metadataRepository.countTopRatedQuestions(request);
    }

    /** Лучшие вопросы, которые в этом упражнении ещё не показывали. */
    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataData> findTopRatedUnusedMetadata(
            @NotNull QuestionBankSearchRequest request, int limit) {
        return toDataWithBodies(metadataRepository.findTopRatedUnusedMetadata(request, limit));
    }

    /** Подходящие вопросы, в том числе уже показанные. */
    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataData> findMetadata(
            @NotNull QuestionBankSearchRequest request, int limit) {
        return toDataWithBodies(metadataRepository.findMetadata(request, limit));
    }

    /** Хоть сколько-нибудь подходящие вопросы: часть критериев может быть не выполнена. */
    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataData> findMetadataRelaxed(
            @NotNull QuestionBankSearchRequest request, int limit) {
        return toDataWithBodies(metadataRepository.findMetadataRelaxed(request, limit));
    }

    /**
     * Метаданные без тел вопроса — для показа списком.
     * <p>
     * Тела не поднимаются намеренно: в статистике по банку от вопроса нужны имя и
     * идентификатор, а тело — самая тяжёлая колонка таблицы.
     */
    @Transactional(readOnly = true)
    public @NotNull List<QuestionMetadataData> findMetadataWithoutBodies(
            @NotNull QuestionBankSearchRequest request, int limit) {
        return metadataRepository.findMetadata(request, limit).stream()
                .map(QuestionMetadataMapping::toData)
                .toList();
    }

    /** Чем были заняты последние вопросы попытки: по ним банк не выдаёт похожий. */
    @Transactional(readOnly = true)
    public @NotNull List<QuestionMaskData> findRecentAttemptQuestionMasks(long attemptId, int limit) {
        return metadataRepository.findRecentAttemptQuestionMasks(attemptId, limit).stream()
                .map(QuestionBankDataRepository::toData)
                .toList();
    }

    /** Вопрос банка вместе с телом; пусто, если такой строки нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<QuestionMetadataData> findMetadataById(int metadataId) {
        return metadataRepository.findByIdFetchingData(metadataId).map(QuestionMetadataMapping::toData);
    }

    // ---------------------------------------------------------------- наполнение банка

    @Transactional(readOnly = true)
    public boolean questionExists(@NotNull String questionName) {
        return metadataRepository.existsByName(questionName);
    }

    @Transactional(readOnly = true)
    public long countByDomain(@NotNull String domainShortname) {
        return metadataRepository.countByDomainShortname(domainShortname);
    }

    /** Какие из перечисленных имён в банке уже заняты. */
    @Transactional(readOnly = true)
    public @NotNull Set<String> findExistingNames(@NotNull String domainShortname,
                                                  @NotNull Collection<String> questionNames) {
        return questionNames.isEmpty() ? Set.of()
                : metadataRepository.findExistingNames(domainShortname, questionNames);
    }

    /** Какие из перечисленных шаблонов в банке уже заняты. */
    @Transactional(readOnly = true)
    public @NotNull Set<String> findExistingTemplateIds(@NotNull String domainShortname,
                                                        @NotNull Collection<String> templateIds) {
        return templateIds.isEmpty() ? Set.of()
                : metadataRepository.findExistingTemplateIds(domainShortname, templateIds);
    }

    /** Из каких источников в банк уже брали вопросы после указанного момента. */
    @Transactional(readOnly = true)
    public @NotNull Set<String> findProcessedOrigins(@NotNull String domainShortname,
                                                     @NotNull LocalDateTime since) {
        return metadataRepository.findProcessedOrigins(domainShortname, since);
    }

    /**
     * Записать сгенерированные вопросы: сначала тела, потом ссылающиеся на них метаданные.
     *
     * @return сколько строк метаданных записано
     */
    @Transactional
    public int saveQuestions(@NotNull List<NewBankQuestionData> questions) {
        var bodies = new ArrayList<QuestionDataEntity>(questions.size());
        var metadata = new ArrayList<QuestionMetadataEntity>();
        for (NewBankQuestionData question : questions) {
            var body = new QuestionDataEntity();
            body.setData(question.body());
            bodies.add(body);
            for (QuestionMetadataData meta : question.metadata()) {
                var entity = QuestionMetadataMapping.toEntity(meta);
                entity.setQuestionData(body);
                metadata.add(entity);
            }
        }
        serializedQuestionRepository.saveAll(bodies);
        metadataRepository.saveAll(metadata);
        return metadata.size();
    }

    /**
     * Заменить тело вопроса в банке новым.
     * <p>
     * Нужно, когда вопрос старого формата пересобирается на лету: метаданные остаются
     * прежними, а тело переписывается. Прежнее тело не удаляется — на него могут
     * ссылаться другие строки метаданных.
     *
     * @throws NoSuchElementException если метаданных с таким id нет
     */
    @Transactional
    public void replaceQuestionBody(int metadataId,
                                    @NotNull SerializableQuestion body) {
        var metadata = metadataRepository.findById(metadataId).orElseThrow(
                () -> new NoSuchElementException("Question metadata " + metadataId + " not found"));
        var entity = new QuestionDataEntity();
        entity.setData(body);
        metadata.setQuestionData(serializedQuestionRepository.save(entity));
        metadataRepository.save(metadata);
    }

    /** Удалить метаданные, заведённые начиная с указанной даты. */
    @Transactional
    public int deleteMetadataFromDate(@NotNull LocalDate date) {
        return metadataRepository.deleteMetadataFromDate(date);
    }

    // ---------------------------------------------------------------- заявки на генерацию

    /** Сколько вопросов по этому запросу уже заказано и ещё не сгенерировано. */
    @Transactional(readOnly = true)
    public int countCurrentlyGeneratingQuestions(@NotNull String domainShortname,
                                                 @NotNull QuestionBankSearchRequest request) {
        return generationRequestRepository.findNumberOfCurrentlyGeneratingQuestions(domainShortname, request);
    }

    /** Незакрытые заявки на генерацию, сгруппированные по одинаковому запросу. */
    @Transactional(readOnly = true)
    public @NotNull List<GenerationRequestGroupData> findActualGenerationRequests(
            @NotNull String domainShortname, @NotNull LocalDateTime createdAfter) {
        return generationRequestRepository.findAllActual(domainShortname, createdAfter);
    }

    /**
     * Заказать генерацию вопросов.
     * <p>
     * В отдельной транзакции: заявка должна пережить откат того, что происходит вокруг
     * поиска, — иначе нехватка вопросов, ради которой её и заводят, останется незамеченной.
     *
     * @return идентификатор заявки
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int createGenerationRequest(@NotNull QuestionBankSearchRequest request,
                                       int questionsToGenerate, @Nullable Long exerciseAttemptId) {
        var entity = new QuestionGenerationRequestEntity(request, questionsToGenerate, exerciseAttemptId);
        return generationRequestRepository.save(entity).getId();
    }

    /** Отметить, что по заявкам сгенерировано столько вопросов, сколько просили. */
    @Transactional
    public void refreshGenerationRequests(@NotNull Integer[] generationRequestIds) {
        generationRequestRepository.updateGenerationRequests(generationRequestIds);
    }

    /** Заявки, которые обработать так и не удалось. */
    @Transactional(readOnly = true)
    public @NotNull List<Integer> findStuckGenerationRequestIds(int processingAttemptsGreaterThan) {
        return generationRequestRepository.findAllIdsByStatusAndProcessingAttemptsGreaterThan(
                QuestionGenerationRequestEntity.Status.ACTUAL, processingAttemptsGreaterThan);
    }

    /** Снять перечисленные заявки. */
    @Transactional
    public void cancelGenerationRequests(@NotNull List<Integer> generationRequestIds) {
        generationRequestRepository.setCancelled(generationRequestIds);
    }

    /** Снять все незакрытые заявки; возвращает их число. */
    @Transactional
    public int cancelAllActiveGenerationRequests() {
        var cancelled = generationRequestRepository.cancelAllActiveRequests();
        return cancelled == null ? 0 : cancelled;
    }

    /** Последняя заявка, заведённая в рамках попытки; пусто, если таких не было. */
    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findLastGenerationRequestIdOfAttempt(long exerciseAttemptId) {
        return generationRequestRepository.getLastRequestByExerciseAttemptId(exerciseAttemptId);
    }

    // ---------------------------------------------------------------- журнал поиска

    /**
     * Записать, чем кончился поиск в банке.
     * <p>
     * В отдельной транзакции по той же причине, что и заявка: журнал ведётся ради
     * разбора неудачных поисков, и откат вокруг не должен его стирать.
     *
     * @return качество последней попытки поиска — им и характеризуется результат
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public @NotNull SearchQuality logSearch(@NotNull QuestionBankSearchRequest request,
                                            @NotNull List<SearchIterationData> iterations,
                                            @Nullable UUID questionRequestId) {
        var entity = new QuestionMetadataSearchRequestEntity(request, iterations, questionRequestId);
        return searchRequestLogRepository.save(entity).getQuality();
    }

    // ---------------------------------------------------------------- маппинг

    /**
     * Метаданные вместе с телами вопросов.
     * <p>
     * Тела поднимаются одним дополнительным запросом на весь список: связь ленивая, и
     * обход результата без него стоил бы по запросу на строку.
     */
    private @NotNull List<QuestionMetadataData> toDataWithBodies(
            @NotNull List<QuestionMetadataEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        metadataRepository.fetchQuestionData(found.stream().map(QuestionMetadataEntity::getId).toList());
        return found.stream().map(QuestionMetadataMapping::toData).toList();
    }

    private static @NotNull QuestionMaskData toData(@NotNull QuestionMaskView view) {
        return new QuestionMaskData(
                view.getConceptBits() == null ? 0L : view.getConceptBits(),
                view.getLawBits() == null ? 0L : view.getLawBits(),
                view.getViolationBits() == null ? 0L : view.getViolationBits(),
                view.getSkillBits() == null ? 0L : view.getSkillBits());
    }
}
