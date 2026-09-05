package org.vstu.compprehension.Service.mapping;

import org.vstu.compprehension.models.data.BackendFactData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.data.*;
import org.vstu.compprehension.models.entities.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Перенос вопроса между JPA-сущностями и {@link QuestionData}.
 * <p>
 * Направление записи намеренно <b>уже</b> направления чтения: читается всё, что нужно
 * доменам, включая взаимодействия студента, а записывается только то, что домены реально
 * меняют. Что именно не переносится обратно и почему — см. {@link #applyToEntity}.
 * <p>
 * У маппера нет зависимостей: ни репозиториев, ни сервисов. Всё, чего нет в данных —
 * например, сущность метаданных из банка заданий, — обязана предоставить вызывающая
 * сторона. Так поведение маппера остаётся очевидным из его сигнатур.
 */
@Component
public class QuestionDataMapper {

    // ---------------------------------------------------------------- чтение

    /**
     * Сущность в данные.
     * <p>
     * Вызывать внутри транзакции: у вопроса ленивые связи, и без открытой сессии обход
     * упадёт. Для этого вопросы поднимаются через {@code findByIdEager}, который
     * подтягивает взаимодействия и feedback одним запросом.
     */
    public static @NotNull QuestionData toData(@NotNull QuestionEntity entity) {
        var data = new QuestionData();
        data.setId(entity.getId());
        data.setQuestionType(entity.getQuestionType());
        data.setQuestionStatus(entity.getQuestionStatus());
        data.setQuestionText(entity.getQuestionText());
        data.setQuestionName(entity.getQuestionName());
        data.setCreatedAt(entity.getCreatedAt());
        data.setQuestionDomainType(entity.getQuestionDomainType());
        data.setOptions(entity.getOptions());
        data.setTags(entity.getTags() == null ? new ArrayList<>() : new ArrayList<>(entity.getTags()));
        data.setMetadata(toData(entity.getMetadata()));
        data.setStatementFacts(copyFacts(entity.getStatementFacts()));
        data.setSolutionFacts(copyFacts(entity.getSolutionFacts()));
        data.setAnswerObjects(map(entity.getAnswerObjects(), QuestionDataMapper::toData));
        data.setInteractions(map(entity.getInteractions(), QuestionDataMapper::toData));
        data.getInteractions().forEach(interaction -> interaction.setQuestion(data));
        return data;
    }

    public static @Nullable AnswerObjectData toData(@Nullable AnswerObjectEntity entity) {
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

    public static @NotNull QuestionInteractionData toData(@NotNull InteractionEntity entity) {
        var data = new QuestionInteractionData();
        data.setId(entity.getId());
        data.setInteractionType(entity.getInteractionType());
        data.setFeedback(entity.getFeedback() == null ? null
                : new FeedbackData(entity.getFeedback().getId(), entity.getFeedback().getGrade(),
                        entity.getFeedback().getInteractionsLeft()));
        data.setViolations(map(entity.getViolations(), QuestionDataMapper::toData));
        data.setCorrectLaw(map(entity.getCorrectLaw(),
                law -> new CorrectLawData(law.getId(), law.getLawName())));
        boolean hasViolations = entity.getViolations() != null && !entity.getViolations().isEmpty();
        data.setResponses(map(entity.getResponses(), response -> {
            var responseData = toData(response);
            responseData.setInteractionHasViolations(hasViolations);
            return responseData;
        }));
        return data;
    }

    public static @NotNull ViolationData toData(@NotNull ViolationEntity entity) {
        var data = new ViolationData();
        data.setId(entity.getId());
        data.setLawName(entity.getLawName());
        data.setDetailedLawName(entity.getDetailedLawName());
        data.setInteractionType(entity.getInteraction() == null ? null
                : entity.getInteraction().getInteractionType());
        data.setViolationFacts(copyFacts(entity.getViolationFacts()));
        data.setExplanationTemplateInfo(map(entity.getExplanationTemplateInfo(),
                t -> new ExplanationTemplateInfoData(t.getId(), t.getFieldName(), t.getValue())));
        return data;
    }

    /**
     * Ответ студента в данные.
     * <p>
     * Из породившего взаимодействия берётся только его тип — единственное, что из него
     * читалось. Обращение инициализирует прокси, но это взаимодействие того же вопроса
     * и уже лежит в контексте персистентности, так что запроса не будет.
     */
    public static @NotNull ResponseData toData(@NotNull ResponseEntity entity) {
        return new ResponseData(
                entity.getId(),
                entity.getSpecValue(),
                toData(entity.getLeftAnswerObject()),
                toData(entity.getRightAnswerObject()),
                entity.getCreatedByInteraction() == null ? null
                        : entity.getCreatedByInteraction().getInteractionType(),
                entity.getCreatedByInteraction() == null ? null
                        : entity.getCreatedByInteraction().getId(),
                entity.getInteraction() != null
                        && entity.getInteraction().getViolations() != null
                        && !entity.getInteraction().getViolations().isEmpty());
    }

    /**
     * Метаданные в данные.
     * <p>
     * Перенесены все поля, кроме {@code generatedBy}: это ссылка на заявку на генерацию,
     * бизнес-логика её не читает (ноль обращений), а тянуть ради неё ещё одну сущность
     * незачем. Поле {@code questionData} перенесено под именем {@code data} — в данных
     * лежит сам сериализованный вопрос, без обёртки-сущности.
     */
    public static @Nullable QuestionMetadataData toData(@Nullable QuestionMetadataEntity entity) {
        if (entity == null) {
            return null;
        }
        var data = new QuestionMetadataData();
        data.setId(entity.getId());
        data.setName(entity.getName());
        data.setDomainShortname(entity.getDomainShortname());
        data.setTemplateId(entity.getTemplateId());
        data.setQDataGraph(entity.getQDataGraph());
        data.setTagBits(entity.getTagBits());
        data.setConceptBits(entity.getConceptBits());
        data.setLawBits(entity.getLawBits());
        data.setSkillBits(entity.getSkillBits());
        data.setViolationBits(entity.getViolationBits());
        data.setTraceConceptBits(entity.getTraceConceptBits());
        data.setSolutionStructuralComplexity(entity.getSolutionStructuralComplexity());
        data.setIntegralComplexity(entity.getIntegralComplexity());
        data.setSolutionSteps(entity.getSolutionSteps());
        data.setDistinctErrorsCount(entity.getDistinctErrorsCount());
        data.setVersion(entity.getVersion());
        data.setStructureHash(entity.getStructureHash());
        data.setOrigin(entity.getOrigin());
        data.setOriginLicense(entity.getOriginLicense());
        data.setCreatedAt(entity.getCreatedAt());
        data.setGenerationRequestId(entity.getGenerationRequestId());
        data.setConceptBitsInPlan(entity.getConceptBitsInPlan());
        data.setConceptBitsInRequest(entity.getConceptBitsInRequest());
        data.setViolationBitsInPlan(entity.getViolationBitsInPlan());
        data.setViolationBitsInRequest(entity.getViolationBitsInRequest());
        data.setSkillBitsInPlan(entity.getSkillBitsInPlan());
        data.setData(entity.getQuestionData() == null ? null : entity.getQuestionData().getData());
        return data;
    }

    /** Шаг цепочки вспомогательных вопросов в данные. */
    public static @NotNull SupplementaryStepData toData(@NotNull SupplementaryStepEntity entity) {
        return SupplementaryStepData.builder()
                .id(entity.getId())
                .mainQuestionInteraction(entity.getMainQuestionInteraction() == null ? null
                        : toData(entity.getMainQuestionInteraction()))
                .situationInfo(entity.getSituationInfo())
                .nextStateId(entity.getNextStateId())
                .build();
    }

    // ---------------------------------------------------------------- запись

    /**
     * Нарушение из данных в сущность.
     * <p>
     * Нарушения создаёт домен по ходу разбора ответа, а записывает их сервис — поэтому
     * перенос нужен в обе стороны. Связь со взаимодействием проставляет вызывающий:
     * у маппера нет доступа к БД.
     */
    public @NotNull ViolationEntity toEntity(@NotNull ViolationData data) {
        var entity = new ViolationEntity();
        entity.setId(data.getId());
        entity.setLawName(data.getLawName());
        entity.setDetailedLawName(data.getDetailedLawName());
        entity.setViolationFacts(data.getViolationFacts() == null
                ? new ArrayList<>() : new ArrayList<>(data.getViolationFacts()));
        entity.setExplanationTemplateInfo(map(data.getExplanationTemplateInfo(), info -> {
            var infoEntity = new ExplanationTemplateInfoEntity();
            infoEntity.setId(info.getId());
            infoEntity.setFieldName(info.getFieldName());
            infoEntity.setValue(info.getValue());
            infoEntity.setViolation(entity);
            return infoEntity;
        }));
        return entity;
    }


    /**
     * Переносит в сущность то, что домены действительно меняют.
     * <p>
     * Намеренно <b>не</b> переносится:
     * <ul>
     *   <li>{@code interactions} — их создаёт и сохраняет сервис, а в
     *       {@link QuestionInteractionData} нет полей {@code orderNumber},
     *       {@code createdAt}, {@code correctLaw} и {@code newResponses}, поэтому
     *       обратный перенос молча их потерял бы;</li>
     *   <li>{@code exerciseAttempt}, {@code domainEntity}, {@code questionRequestLog} —
     *       их проставляет сервис, в данных вопроса их нет вовсе;</li>
     *   <li>{@code createdAt} — проставляется базой при вставке.</li>
     * </ul>
     * Существующие варианты ответа (с непустым id) обновляются на месте, новые
     * добавляются. Удаление вариантов не поддерживается: домены их не удаляют.
     */
    public void applyToEntity(@NotNull QuestionData data, @NotNull QuestionEntity target,
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

    /**
     * Новая сущность вопроса по данным.
     *
     * @param metadata строка метаданных банка, если вопрос из банка; резолвит её вызывающий
     */
    public @NotNull QuestionEntity toNewEntity(@NotNull QuestionData data,
                                               @Nullable QuestionMetadataEntity metadata) {
        var entity = new QuestionEntity();
        entity.setAnswerObjects(new ArrayList<>());
        entity.setInteractions(new ArrayList<>());
        applyToEntity(data, entity, metadata);
        return entity;
    }

    private void applyAnswerObjects(QuestionData data, QuestionEntity target) {
        if (data.getAnswerObjects() == null) {
            return;
        }
        if (target.getAnswerObjects() == null) {
            target.setAnswerObjects(new ArrayList<>());
        }
        Map<Long, AnswerObjectEntity> existing = target.getAnswerObjects().stream()
                .filter(a -> a.getId() != null)
                .collect(Collectors.toMap(AnswerObjectEntity::getId, Function.identity(), (a, b) -> a));

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

    // ---------------------------------------------------------------- утилиты

    private static List<BackendFactData> copyFacts(@Nullable List<BackendFactData> facts) {
        return facts == null ? new ArrayList<>() : new ArrayList<>(facts);
    }

    private static <S, T> List<T> map(@Nullable List<S> source, Function<S, T> mapper) {
        return source == null ? new ArrayList<>()
                : source.stream().map(mapper).collect(Collectors.toCollection(ArrayList::new));
    }
}
