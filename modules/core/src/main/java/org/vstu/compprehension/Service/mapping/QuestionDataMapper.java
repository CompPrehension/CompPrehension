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
 * Читается отсюда только то, что не приходит из базы графом: метаданные банка заданий
 * (их собирают в памяти при генерации) и только что созданные ответы студента. Подъём
 * сохранённого вопроса живёт в {@code models.repository.data}, рядом со своим запросом:
 * маппер не знает, какой выборкой подняли сущность, и потому не может отличить пустую
 * ссылку от неподгруженной.
 * <p>
 * Направление записи намеренно уже направления чтения: переносится только то, что домены
 * действительно меняют. Что не переносится и почему — см. {@link #applyToEntity}.
 * <p>
 * У маппера нет зависимостей: ни репозиториев, ни сервисов. Всё, чего нет в данных,
 * обязана предоставить вызывающая сторона.
 */
@Component
public class QuestionDataMapper {

    // ---------------------------------------------------------------- чтение

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

    /**
     * Только что созданный ответ студента — в данные.
     * <p>
     * Имя говорит о форме: сюда приходят ответы, собранные сервисом по запросу с фронта
     * и ещё не сохранённые. Взаимодействия у такого ответа нет — оно создаётся позже,
     * из результата разбора, — поэтому {@code interactionHasViolations} всегда false.
     * Сохранённые ответы поднимаются вместе с вопросом в {@code models.repository.data}.
     * <p>
     * Варианты ответа и породившее взаимодействие — уже поднятые сущности: их сервис
     * нашёл по идентификаторам из запроса.
     */
    public static @NotNull ResponseData toNewResponseData(@NotNull ResponseEntity entity) {
        var createdBy = entity.getCreatedByInteraction();
        return new ResponseData(
                entity.getId(),
                entity.getSpecValue(),
                toData(entity.getLeftAnswerObject()),
                toData(entity.getRightAnswerObject()),
                createdBy == null ? null : createdBy.getInteractionType(),
                createdBy == null ? null : createdBy.getId(),
                false);
    }

    /** Вариант ответа; null допускает сама схема — внешние ключи ответа не обязательны. */
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
