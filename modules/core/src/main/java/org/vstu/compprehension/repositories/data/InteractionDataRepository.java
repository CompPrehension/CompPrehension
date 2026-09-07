package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.ExplanationTemplateInfoData;
import org.vstu.compprehension.data.question.InteractionResponsesData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.RecordedInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.ExplanationTemplateInfoEntity;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.ResponseRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Взаимодействия студента с вопросом: ответы, нарушения, оценка.
 * <p>
 * Отдельно от {@link QuestionDataRepository}, потому что тот отвечает на вопрос «как
 * вопрос выглядит сейчас», а этот — «что студент с ним делал». Читаются они в разных
 * местах и разными выборками: разбор ответа не поднимает ни текст вопроса, ни его
 * метаданные.
 * <p>
 * Ответ студента становится строкой в БД только вместе со взаимодействием. Раньше
 * ответы сохранялись сразу при разборе запроса, и у вспомогательных вопросов, которые
 * взаимодействий не записывают, оставались строки, на которые никто не ссылается.
 */
@Repository
@RequiredArgsConstructor
public class InteractionDataRepository {

    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ResponseRepository responseRepository;

    /**
     * Ответы, пришедшие с фронта, в вид, с которым работают домены.
     * <p>
     * Номера вариантов превращаются в сами варианты одним запросом. В БД ничего не
     * пишется: разбор ответа может ничем не кончиться — например, у вспомогательного
     * вопроса, — и след в базе от этого оставаться не должен.
     *
     * @throws NoSuchElementException   если вопроса нет
     * @throws IllegalArgumentException если у вопроса нет варианта с таким номером
     */
    @Transactional(readOnly = true)
    public @NotNull List<ResponseData> resolveAnswers(long questionId,
                                                      @NotNull List<SubmittedAnswerData> answers) {
        var answerObjects = answerObjectsByAnswerId(findQuestion(questionId), questionId);
        return answers.stream()
                .map(answer -> new ResponseData(
                        null,
                        null,
                        toData(requireAnswerObject(answerObjects, answer.leftAnswerId(), questionId)),
                        toData(requireAnswerObject(answerObjects, answer.rightAnswerId(), questionId)),
                        // Тип породившего взаимодействия здесь не нужен: домены читают
                        // его только у сохранённых ответов, поднятых вместе с вопросом.
                        null,
                        answer.createdByInteractionId(),
                        false))
                .toList();
    }

    /**
     * Последнее взаимодействие, после которого вопрос ещё можно продолжать: без нарушений
     * и с неисчерпанным остатком попыток.
     *
     * @return пусто, если студент ещё не отвечал верно
     */
    @Transactional(readOnly = true)
    public @NotNull Optional<InteractionResponsesData> findLatestCorrectInteraction(long questionId) {
        var interactions = loadInteractions(questionId);
        return latestCorrect(interactions).map(InteractionDataRepository::toResponses);
    }

    /**
     * Записать взаимодействие вместе с его ответами и нарушениями.
     * <p>
     * Одним вызовом, потому что по отдельности эти записи не имеют смысла: ответ без
     * взаимодействия ни на что не ссылается.
     * <p>
     * Оценки здесь нет намеренно. Стратегия считает её по истории попытки, в которую
     * входит и это взаимодействие, поэтому сначала запись, потом {@link #grade}.
     * Между двумя вызовами взаимодействие существует с нулевой оценкой — так было
     * и раньше, просто это состояние не было названо.
     *
     * @throws NoSuchElementException   если вопроса или переносимого ответа нет
     * @throws IllegalArgumentException если у вопроса нет варианта с таким номером
     */
    @Transactional
    public @NotNull RecordedInteractionData record(@NotNull NewInteractionData data) {
        var question = findQuestion(data.questionId());
        var answerObjects = answerObjectsByAnswerId(question, data.questionId());

        var responses = new ArrayList<ResponseEntity>();
        for (Long responseId : data.carriedResponseIds()) {
            responses.add(responseRepository.findById(responseId).orElseThrow(
                    () -> new NoSuchElementException("Response " + responseId + " not found")));
        }
        for (SubmittedAnswerData answer : data.answers()) {
            var response = new ResponseEntity();
            response.setLeftAnswerObject(requireAnswerObject(answerObjects, answer.leftAnswerId(), data.questionId()));
            response.setRightAnswerObject(requireAnswerObject(answerObjects, answer.rightAnswerId(), data.questionId()));
            if (answer.createdByInteractionId() != null) {
                response.setCreatedByInteraction(
                        interactionRepository.getReferenceById(answer.createdByInteractionId()));
            }
            responses.add(response);
        }
        // Ответ, о котором фронт не сказал, откуда он, считается данным сейчас:
        // конструктор взаимодействия проставит ему ссылку на себя.
        var firstGivenHere = responses.stream()
                .filter(response -> response.getCreatedByInteraction() == null)
                .toList();

        var interaction = new InteractionEntity(
                data.interactionType(),
                question,
                data.violations().stream().map(InteractionDataRepository::toEntity).toList(),
                data.correctLaws(),
                responses,
                firstGivenHere);
        interaction.getFeedback().setInteractionsLeft(data.interactionsLeft());

        question.getInteractions().add(interaction);
        interactionRepository.save(interaction);
        // Идентификаторы нужны сразу: счётчики ниже считаются запросом, который
        // неотправленных вставок не увидит.
        interactionRepository.flush();

        var interactions = loadInteractions(data.questionId());
        long correct = interactions.stream().filter(i -> i.getViolations().isEmpty()).count();

        return new RecordedInteractionData(
                interaction.getId(),
                (int) correct,
                interactions.size() - (int) correct,
                toResponses(interaction).responses(),
                latestCorrect(interactions).map(InteractionDataRepository::toResponses).orElse(null));
    }

    /**
     * Выставить оценку за взаимодействие.
     *
     * @throws NoSuchElementException если взаимодействия нет
     */
    @Transactional
    public void grade(long interactionId, float grade) {
        var interaction = interactionRepository.findById(interactionId).orElseThrow(
                () -> new NoSuchElementException("Interaction " + interactionId + " not found"));
        Strict.required(interaction.getFeedback(), "feedback", "interaction " + interactionId)
                .setGrade(grade);
        interactionRepository.save(interaction);
    }

    // ---------------------------------------------------------------- внутреннее

    private @NotNull QuestionEntity findQuestion(long questionId) {
        return questionRepository.findByIdFetchingAnswerObjects(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));
    }

    /**
     * Взаимодействия вопроса с оценкой, нарушениями и ответами.
     * <p>
     * Два запроса, а не один: несколько List-коллекций в одном join fetch — это
     * MultipleBagFetchException. Оба попадают в один контекст персистентности, поэтому
     * после них у взаимодействий инициализированы обе коллекции.
     */
    private @NotNull List<InteractionEntity> loadInteractions(long questionId) {
        interactionRepository.findAllByQuestionIdFetchingViolations(questionId);
        return interactionRepository.findAllByQuestionIdFetchingResponses(questionId).stream()
                .sorted(Comparator.comparing(InteractionEntity::getId))
                .toList();
    }

    private static @NotNull Optional<InteractionEntity> latestCorrect(
            @NotNull List<InteractionEntity> interactions) {
        return interactions.stream()
                .filter(i -> i.getFeedback() != null && i.getFeedback().getInteractionsLeft() >= 0)
                .filter(i -> i.getViolations().isEmpty())
                .reduce((first, second) -> second);
    }

    private static @NotNull Map<Integer, AnswerObjectEntity> answerObjectsByAnswerId(
            @NotNull QuestionEntity question, long questionId) {
        var answerObjects = Strict.required(question.getAnswerObjects(), "answerObjects",
                "question " + questionId);
        return answerObjects.stream().collect(Collectors.toMap(
                AnswerObjectEntity::getAnswerId, Function.identity(), (a, b) -> a));
    }

    private static @NotNull AnswerObjectEntity requireAnswerObject(
            @NotNull Map<Integer, AnswerObjectEntity> answerObjects, int answerId, long questionId) {
        var answerObject = answerObjects.get(answerId);
        if (answerObject == null) {
            throw new IllegalArgumentException(
                    "Question " + questionId + " has no answer object " + answerId);
        }
        return answerObject;
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull InteractionResponsesData toResponses(@NotNull InteractionEntity interaction) {
        boolean hasViolations = !interaction.getViolations().isEmpty();
        return new InteractionResponsesData(
                interaction.getId(),
                interaction.getResponses().stream()
                        .map(response -> toData(response, hasViolations))
                        .toList());
    }

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

    /**
     * Нарушение из данных в сущность.
     * <p>
     * Нарушения создаёт домен по ходу разбора ответа, а записываются они здесь вместе
     * со взаимодействием — связь с ним проставляет конструктор {@link InteractionEntity}.
     */
    private static @NotNull ViolationEntity toEntity(@NotNull ViolationData data) {
        var entity = new ViolationEntity();
        entity.setId(data.getId());
        entity.setLawName(data.getLawName());
        entity.setDetailedLawName(data.getDetailedLawName());
        entity.setViolationFacts(data.getViolationFacts() == null
                ? new ArrayList<>() : new ArrayList<>(data.getViolationFacts()));
        entity.setExplanationTemplateInfo(toEntities(data.getExplanationTemplateInfo(), entity));
        return entity;
    }

    private static @NotNull List<ExplanationTemplateInfoEntity> toEntities(
            @Nullable List<ExplanationTemplateInfoData> source, @NotNull ViolationEntity owner) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream().map(info -> {
            var entity = new ExplanationTemplateInfoEntity();
            entity.setId(info.getId());
            entity.setFieldName(info.getFieldName());
            entity.setValue(info.getValue());
            entity.setViolation(owner);
            return entity;
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}
