package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.BackendFactData;
import org.vstu.compprehension.data.question.CorrectLawData;
import org.vstu.compprehension.data.question.ExplanationTemplateInfoData;
import org.vstu.compprehension.data.question.FeedbackData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.data.question.QuestionRequestLogData;
import org.vstu.compprehension.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.repositories.entity.AnswerObjectRepository;
import org.vstu.compprehension.repositories.entity.DomainRepository;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.QuestionRequestLogRepository;
import org.vstu.compprehension.repositories.entity.ViolationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Вопрос целиком, в виде отсоединённых данных.
 * <p>
 * Ровно шесть запросов независимо от того, сколько у вопроса взаимодействий и ответов.
 * Форма выборки задана здесь же, рядом с маппингом, поэтому маппинг тотальный: каждое
 * поле {@link QuestionData} заполнено, кроме тех, что допускают null в самой схеме
 * (метаданные, оценка взаимодействия, породившее ответ взаимодействие).
 * <p>
 * Раньше это делалось связкой {@code findByIdEager} + маппером вопроса:
 * запрос поднимал вопрос со взаимодействиями, а всё остальное маппер добирал обходом
 * ленивых связей — по запросу на каждую коллекцию каждого взаимодействия.
 */
@Repository
@RequiredArgsConstructor
public class QuestionDataRepository {

    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ViolationRepository violationRepository;
    private final AnswerObjectRepository answerObjectRepository;
    private final DomainRepository domainRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionRequestLogRepository questionRequestLogRepository;

    /**
     * Вопрос со всеми взаимодействиями, ответами и нарушениями.
     *
     * @throws NoSuchElementException если вопроса нет
     */
    @Transactional(readOnly = true)
    public @NotNull QuestionData findById(long questionId) {
        var question = questionRepository.findByIdFetchingMetadata(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));

        // Дальше — та же сущность из контекста персистентности, у которой запросы
        // по очереди инициализируют коллекции. Результаты не нужны: важен побочный
        // эффект, после которого обход графа не порождает запросов.
        questionRepository.findByIdFetchingAnswerObjects(questionId);
        violationRepository.findAllByQuestionIdFetchingTemplates(questionId);
        var interactions = interactionRepository.findAllByQuestionIdFetchingViolations(questionId);
        interactionRepository.findAllByQuestionIdFetchingResponses(questionId);
        interactionRepository.findAllByQuestionIdFetchingCorrectLaws(questionId);

        return toData(question, interactions);
    }

    /**
     * Одно взаимодействие вместе с вопросом, которому оно принадлежит.
     * <p>
     * Взаимодействие не собирается отдельно, а берётся из полного вопроса: только так
     * обратная ссылка {@link QuestionInteractionData#getQuestion()} заполнена всегда.
     * Именно на ней раньше падали домены, получая шаг цепочки вспомогательных вопросов.
     *
     * @throws NoSuchElementException если взаимодействия нет
     */
    @Transactional(readOnly = true)
    public @NotNull QuestionInteractionData findInteractionById(long interactionId) {
        long questionId = interactionRepository.findQuestionId(interactionId)
                .orElseThrow(() -> new NoSuchElementException("Interaction " + interactionId + " not found"));
        return findById(questionId).getInteractions().stream()
                .filter(i -> Objects.equals(i.getId(), interactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Interaction " + interactionId + " is not among interactions of question " + questionId));
    }

    /**
     * Имя предметной области вопроса — скалярным запросом, без подъёма самого вопроса.
     *
     * @throws NoSuchElementException если вопроса нет
     */
    @Transactional(readOnly = true)
    public @NotNull String getDomainName(long questionId) {
        return questionRepository.findDomainName(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));
    }

    /** Владелец попытки, породившей вопрос; пусто, если вопрос задан вне попытки. */
    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findOwnerUserId(long questionId) {
        return questionRepository.findOwnerUserId(questionId);
    }

    /**
     * Записать вопрос: новый завести, существующий обновить на месте.
     * <p>
     * Единственный сток всех путей генерации, поэтому здесь же проставляются связи,
     * которых в данных вопроса нет и быть не может: домен, попытка и журнал запроса.
     * Идентификаторы, назначенные базой, возвращаются в переданные данные —
     * вызывающий продолжает работать с тем же объектом вопроса.
     *
     * @param domainId          предметная область вопроса; известна вызывающему из домена
     * @param questionRequestLog журнал запроса, если вопрос сгенерирован по запросу;
     *                           записывается раньше вопроса — связь идёт по его id
     * @param exerciseAttemptId  попытка, в рамках которой задан вопрос, если она есть
     * @return идентификатор записанного вопроса
     */
    @Transactional
    public long save(@NotNull QuestionData data, @NotNull String domainId,
                     @Nullable QuestionRequestLogData questionRequestLog,
                     @Nullable Long exerciseAttemptId) {
        // Метаданные приходят из банка заданий и уже существуют, поэтому берутся
        // ссылкой по идентификатору, без запроса.
        var metadata = data.getMetadata() == null || data.getMetadata().getId() == null
                ? null
                : questionMetadataRepository.getReferenceById(data.getMetadata().getId());

        // Идентификатор есть только у вопросов, поднятых из БД: по нему и решается,
        // обновлять существующую строку или заводить новую.
        var entity = data.getId() == null
                ? toNewEntity(data, metadata)
                : questionRepository.findById(data.getId())
                        .orElseGet(() -> toNewEntity(data, metadata));
        if (data.getId() != null) {
            applyToEntity(data, entity, metadata);
        }

        if (questionRequestLog != null) {
            entity.setQuestionRequestLog(questionRequestLogRepository.save(toEntity(questionRequestLog)));
        }
        if (exerciseAttemptId != null) {
            entity.setExerciseAttempt(exerciseAttemptRepository.getReferenceById(exerciseAttemptId));
        }
        if (entity.getDomainEntity() == null) {
            entity.setDomainEntity(domainRepository.getReferenceById(domainId));
        }

        questionRepository.save(entity);

        // Варианты ответа сохраняются после вопроса: у новых связь идёт по его id.
        if (entity.getAnswerObjects() != null) {
            for (AnswerObjectEntity answerObject : entity.getAnswerObjects()) {
                if (answerObject.getQuestion() == null) {
                    answerObject.setQuestion(entity);
                }
            }
            answerObjectRepository.saveAll(
                    entity.getAnswerObjects().stream().filter(a -> a.getId() == null)::iterator);
        }

        data.setId(entity.getId());
        for (int i = 0; i < entity.getAnswerObjects().size() && i < data.getAnswerObjects().size(); i++) {
            data.getAnswerObjects().get(i).setId(entity.getAnswerObjects().get(i).getId());
        }
        return entity.getId();
    }

    // ---------------------------------------------------------------- маппинг

    /**
     * Новая сущность вопроса по данным.
     *
     * @param metadata строка метаданных банка, если вопрос из банка
     */
    private static @NotNull QuestionEntity toNewEntity(
            @NotNull QuestionData data,
            @Nullable QuestionMetadataEntity metadata) {
        var entity = new QuestionEntity();
        entity.setAnswerObjects(new ArrayList<>());
        entity.setInteractions(new ArrayList<>());
        applyToEntity(data, entity, metadata);
        return entity;
    }

    /**
     * Переносит в сущность то, что домены действительно меняют.
     * <p>
     * Намеренно <b>не</b> переносится:
     * <ul>
     *   <li>{@code interactions} — их пишет {@link InteractionDataRepository}, а в
     *       {@link QuestionInteractionData} нет полей {@code orderNumber},
     *       {@code createdAt}, {@code correctLaw} и {@code newResponses}, поэтому
     *       обратный перенос молча их потерял бы;</li>
     *   <li>{@code exerciseAttempt}, {@code domainEntity}, {@code questionRequestLog} —
     *       их проставляет {@link #save}, в данных вопроса их нет вовсе;</li>
     *   <li>{@code createdAt} — проставляется базой при вставке.</li>
     * </ul>
     * Существующие варианты ответа (с непустым id) обновляются на месте, новые
     * добавляются. Удаление вариантов не поддерживается: домены их не удаляют.
     */
    private static void applyToEntity(
            @NotNull QuestionData data, @NotNull QuestionEntity target,
            @Nullable QuestionMetadataEntity metadata) {
        target.setQuestionType(data.getQuestionType());
        target.setQuestionStatus(data.getQuestionStatus());
        target.setQuestionText(data.getQuestionText());
        target.setQuestionName(data.getQuestionName());
        target.setQuestionDomainType(data.getQuestionDomainType());
        target.setOptions(data.getOptions());
        target.setTags(data.getTags() == null ? new ArrayList<>() : new ArrayList<>(data.getTags()));
        target.setStatementFacts(copyFacts(data.getStatementFacts()));
        target.setSolutionFacts(copyFacts(data.getSolutionFacts()));
        target.setMetadata(metadata);
        applyAnswerObjects(data, target);
    }

    private static void applyAnswerObjects(@NotNull QuestionData data, @NotNull QuestionEntity target) {
        if (data.getAnswerObjects() == null) {
            return;
        }
        if (target.getAnswerObjects() == null) {
            target.setAnswerObjects(new ArrayList<>());
        }
        var existing = target.getAnswerObjects().stream()
                .filter(a -> a.getId() != null)
                .collect(Collectors.toMap(AnswerObjectEntity::getId, a -> a, (a, b) -> a));

        for (AnswerObjectData source : data.getAnswerObjects()) {
            var entity = source.getId() == null ? null : existing.get(source.getId());
            if (entity == null) {
                entity = new AnswerObjectEntity();
                entity.setQuestion(target);
                target.getAnswerObjects().add(entity);
            }
            entity.setAnswerId(source.getAnswerId());
            entity.setHyperText(source.getHyperText());
            entity.setDomainInfo(source.getDomainInfo());
            entity.setRightCol(source.isRightCol());
            entity.setConcept(source.getConcept());
        }
    }

    /** Журнал запроса — плоская копия, связей у него нет. */
    private static @NotNull QuestionRequestLogEntity toEntity(@NotNull QuestionRequestLogData data) {
        return QuestionRequestLogEntity.builder()
                .id(data.getId())
                .exerciseAttemptId(data.getExerciseAttemptId())
                .domainShortname(data.getDomainShortname())
                .targetConceptNames(data.getTargetConceptNames())
                .deniedConceptNames(data.getDeniedConceptNames())
                .allowedConceptNames(data.getAllowedConceptNames())
                .targetLawNames(data.getTargetLawNames())
                .deniedLawNames(data.getDeniedLawNames())
                .allowedLawNames(data.getAllowedLawNames())
                .targetSkillNames(data.getTargetSkillNames())
                .deniedSkillNames(data.getDeniedSkillNames())
                .allowedSkillNames(data.getAllowedSkillNames())
                .targetTags(data.getTargetTags())
                .conceptsTargetedBitmask(data.getConceptsTargetedBitmask())
                .conceptsDeniedBitmask(data.getConceptsDeniedBitmask())
                .lawsTargetedBitmask(data.getLawsTargetedBitmask())
                .lawsDeniedBitmask(data.getLawsDeniedBitmask())
                .skillsDeniedBitmask(data.getSkillsDeniedBitmask())
                .skillsTargetedBitmask(data.getSkillsTargetedBitmask())
                .targetTagsBitmask(data.getTargetTagsBitmask())
                .deniedQuestionNames(data.getDeniedQuestionNames())
                .deniedQuestionTemplateIds(data.getDeniedQuestionTemplateIds())
                .deniedQuestionMetaIds(data.getDeniedQuestionMetaIds())
                .solvingDuration(data.getSolvingDuration())
                .complexity(data.getComplexity())
                .stepsMin(data.getStepsMin())
                .stepsMax(data.getStepsMax())
                .complexitySearchDirection(data.getComplexitySearchDirection())
                .lawsSearchDirection(data.getLawsSearchDirection())
                .chanceToPickAutogeneratedQuestion(data.getChanceToPickAutogeneratedQuestion())
                .build();
    }

    private static @NotNull QuestionData toData(@NotNull QuestionEntity entity,
                                                @NotNull List<InteractionEntity> interactions) {
        var data = new QuestionData();
        data.setId(entity.getId());
        data.setQuestionType(entity.getQuestionType());
        data.setQuestionStatus(entity.getQuestionStatus());
        data.setQuestionText(entity.getQuestionText());
        data.setQuestionName(entity.getQuestionName());
        data.setCreatedAt(entity.getCreatedAt());
        data.setQuestionDomainType(entity.getQuestionDomainType());
        data.setOptions(entity.getOptions());
        data.setTags(new ArrayList<>(entity.getTags()));
        // Метаданные — плоская копия полей плюс тело из банка, поднятое тем же запросом.
        // Маппинг общий с путём генерации из банка: там метаданные приходят собранными
        // в памяти, и он одинаково тотален в обоих случаях.
        data.setMetadata(QuestionMetadataMapping.toData(entity.getMetadata()));
        data.setStatementFacts(copyFacts(entity.getStatementFacts()));
        data.setSolutionFacts(copyFacts(entity.getSolutionFacts()));
        data.setAnswerObjects(entity.getAnswerObjects().stream()
                .map(QuestionDataRepository::toData)
                .collect(Collectors.toCollection(ArrayList::new)));

        // Порядок задан явно: у коллекции взаимодействий нет @OrderBy, а домены берут
        // последнее взаимодействие по позиции в списке.
        var interactionsData = interactions.stream()
                .sorted(Comparator.comparing(InteractionEntity::getId))
                .map(QuestionDataRepository::toData)
                .collect(Collectors.toCollection(ArrayList::new));
        interactionsData.forEach(interaction -> interaction.setQuestion(data));
        data.setInteractions(interactionsData);
        return data;
    }

    /**
     * Вариант ответа.
     * <p>
     * Null допускает сама схема: у ответа студента внешние ключи на варианты не
     * обязательны. Это не «может быть не подгружено», а реально пустая ссылка.
     */
    private static @Nullable AnswerObjectData toData(@Nullable AnswerObjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return AnswerObjectData.builder()
                .id(entity.getId())
                .answerId(entity.getAnswerId())
                .hyperText(entity.getHyperText())
                .domainInfo(entity.getDomainInfo())
                .isRightCol(entity.isRightCol())
                .concept(entity.getConcept())
                .build();
    }

    private static @NotNull QuestionInteractionData toData(@NotNull InteractionEntity entity) {
        var data = new QuestionInteractionData();
        data.setId(entity.getId());
        data.setInteractionType(entity.getInteractionType());
        // Оценка допускает отсутствие: связь объявлена с @NotFound(IGNORE), и на висячую
        // ссылку Hibernate подставляет null вместо ошибки.
        data.setFeedback(entity.getFeedback() == null ? null
                : new FeedbackData(entity.getFeedback().getId(), entity.getFeedback().getGrade(),
                        entity.getFeedback().getInteractionsLeft()));
        data.setViolations(entity.getViolations().stream()
                .map(violation -> toData(violation, entity))
                .collect(Collectors.toCollection(ArrayList::new)));
        data.setCorrectLaw(entity.getCorrectLaw().stream()
                .map(law -> new CorrectLawData(law.getId(), law.getLawName()))
                .collect(Collectors.toCollection(ArrayList::new)));

        boolean hasViolations = !entity.getViolations().isEmpty();
        data.setResponses(entity.getResponses().stream()
                .map(response -> toData(response, hasViolations))
                .collect(Collectors.toCollection(ArrayList::new)));
        return data;
    }

    /** @param owner взаимодействие, которому нарушение принадлежит — из него берётся тип */
    private static @NotNull ViolationData toData(@NotNull ViolationEntity entity,
                                                 @NotNull InteractionEntity owner) {
        var data = new ViolationData();
        data.setId(entity.getId());
        data.setLawName(entity.getLawName());
        data.setDetailedLawName(entity.getDetailedLawName());
        data.setInteractionType(owner.getInteractionType());
        data.setViolationFacts(copyFacts(entity.getViolationFacts()));
        data.setExplanationTemplateInfo(entity.getExplanationTemplateInfo().stream()
                .map(t -> new ExplanationTemplateInfoData(t.getId(), t.getFieldName(), t.getValue()))
                .collect(Collectors.toCollection(ArrayList::new)));
        return data;
    }

    /** @param interactionHasViolations были ли нарушения во взаимодействии-владельце */
    private static @NotNull ResponseData toData(@NotNull ResponseEntity entity,
                                                boolean interactionHasViolations) {
        var createdBy = entity.getCreatedByInteraction();
        return new ResponseData(
                entity.getId(),
                entity.getSpecValue(),
                toData(entity.getLeftAnswerObject()),
                toData(entity.getRightAnswerObject()),
                createdBy == null ? null : createdBy.getInteractionType(),
                createdBy == null ? null : createdBy.getId(),
                interactionHasViolations);
    }

    private static @NotNull List<BackendFactData> copyFacts(@Nullable List<BackendFactData> facts) {
        return facts == null ? new ArrayList<>() : new ArrayList<>(facts);
    }
}
