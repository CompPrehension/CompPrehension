package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.InteractionResponsesData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.RecordedInteractionData;
import org.vstu.compprehension.data.question.ResponseData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.Strict;
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

@Repository
@RequiredArgsConstructor
public class InteractionDataRepository {

    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ResponseRepository responseRepository;
    private final Mapper<AnswerObjectEntity, AnswerObjectData> answerObjectMapper;
    private final Mapper<InteractionEntity, InteractionResponsesData> interactionResponsesMapper;
    private final Mapper<ViolationData, ViolationEntity> violationEntityMapper;

    @Transactional(readOnly = true)
    public @NotNull List<ResponseData> resolveAnswers(long questionId,
                                                      @NotNull List<SubmittedAnswerData> answers) {
        var answerObjects = answerObjectsByAnswerId(findQuestion(questionId), questionId);
        return answers.stream()
                .map(answer -> new ResponseData(
                        null,
                        null,
                        answerObjectMapper.map(
                                requireAnswerObject(answerObjects, answer.leftAnswerId(), questionId)),
                        answerObjectMapper.map(
                                requireAnswerObject(answerObjects, answer.rightAnswerId(), questionId)),
                        null,
                        answer.createdByInteractionId(),
                        false))
                .toList();
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<InteractionResponsesData> findLatestCorrectInteraction(long questionId) {
        var interactions = loadInteractions(questionId);
        return latestCorrect(interactions).map(interactionResponsesMapper::map);
    }

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
        var firstGivenHere = responses.stream()
                .filter(response -> response.getCreatedByInteraction() == null)
                .toList();

        var interaction = new InteractionEntity(
                data.interactionType(),
                question,
                violationEntityMapper.mapAll(data.violations()),
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
                interactionResponsesMapper.map(interaction).responses(),
                latestCorrect(interactions).map(interactionResponsesMapper::map).orElse(null));
    }

    @Transactional
    public void grade(long interactionId, float grade) {
        var interaction = interactionRepository.findById(interactionId).orElseThrow(
                () -> new NoSuchElementException("Interaction " + interactionId + " not found"));
        Strict.required(interaction.getFeedback(), "feedback", "interaction " + interactionId)
                .setGrade(grade);
        interactionRepository.save(interaction);
    }

    private @NotNull QuestionEntity findQuestion(long questionId) {
        return questionRepository.findByIdFetchingAnswerObjects(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));
    }

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
}
