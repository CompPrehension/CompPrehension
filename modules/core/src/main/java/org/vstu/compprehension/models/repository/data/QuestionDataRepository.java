package org.vstu.compprehension.models.repository.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.Service.mapping.QuestionDataMapper;
import org.vstu.compprehension.models.data.AnswerObjectData;
import org.vstu.compprehension.models.data.BackendFactData;
import org.vstu.compprehension.models.data.CorrectLawData;
import org.vstu.compprehension.models.data.ExplanationTemplateInfoData;
import org.vstu.compprehension.models.data.FeedbackData;
import org.vstu.compprehension.models.data.QuestionData;
import org.vstu.compprehension.models.data.QuestionInteractionData;
import org.vstu.compprehension.models.data.ResponseData;
import org.vstu.compprehension.models.data.ViolationData;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.InteractionEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.ViolationEntity;
import org.vstu.compprehension.models.repository.InteractionRepository;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.models.repository.ViolationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Вопрос целиком, в виде отсоединённых данных.
 * <p>
 * Ровно шесть запросов независимо от того, сколько у вопроса взаимодействий и ответов.
 * Форма выборки задана здесь же, рядом с маппингом, поэтому маппинг тотальный: каждое
 * поле {@link QuestionData} заполнено, кроме тех, что допускают null в самой схеме
 * (метаданные, оценка взаимодействия, породившее ответ взаимодействие).
 * <p>
 * Раньше это делалось связкой {@code findByIdEager} + {@code QuestionDataMapper.toData}:
 * запрос поднимал вопрос со взаимодействиями, а всё остальное маппер добирал обходом
 * ленивых связей — по запросу на каждую коллекцию каждого взаимодействия.
 */
@Repository
@RequiredArgsConstructor
public class QuestionDataRepository {

    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ViolationRepository violationRepository;

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

    // ---------------------------------------------------------------- маппинг

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
        data.setMetadata(QuestionDataMapper.toData(entity.getMetadata()));
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
