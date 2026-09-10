package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.NewInteractionData;
import org.vstu.compprehension.data.question.NewInteractionAnswerData;
import org.vstu.compprehension.data.question.QuestionInteractionData;
import org.vstu.compprehension.data.question.SubmittedAnswerData;
import org.vstu.compprehension.data.question.ViolationData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.ResponseEntity;
import org.vstu.compprehension.entities.ViolationEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.utils.Strict;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.ResponseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InteractionDataRepository {

    private final QuestionRepository questionRepository;
    private final InteractionRepository interactionRepository;
    private final ResponseRepository responseRepository;
    private final Mapper<AnswerObjectEntity, AnswerObjectData> answerObjectMapper;
    private final Mapper<InteractionEntity, QuestionInteractionData> questionInteractionMapper;
    private final Mapper<ViolationData, ViolationEntity> violationEntityMapper;

    @Transactional(readOnly = true)
    public @NotNull List<AnswerData> resolveAnswers(long questionId, @NotNull List<SubmittedAnswerData> answers) {
        var answerObjects = answerObjectsByAnswerId(findQuestion(questionId), questionId);
        return answers.stream()
                .map(answer -> AnswerData.of(
                        answerObjectMapper.map(
                                requireAnswerObject(answerObjects, answer.leftAnswerId(), questionId)),
                        answerObjectMapper.map(
                                requireAnswerObject(answerObjects, answer.rightAnswerId(), questionId))))
                .toList();
    }

    @Transactional
    public @NotNull QuestionInteractionData record(@NotNull NewInteractionData data) {
        var question = findQuestion(data.questionId());
        var answerObjects = answerObjectsByAnswerId(question, data.questionId());

        var responses = new ArrayList<ResponseEntity>();
        for (NewInteractionAnswerData answer : data.answers()) {
            if (answer.responseId() != null) {
                responses.add(existingResponse(answer.responseId()));
                continue;
            }
            var response = new ResponseEntity();
            response.setLeftAnswerObject(
                    requireAnswerObject(answerObjects, answer.leftAnswerId(), data.questionId()));
            response.setRightAnswerObject(
                    requireAnswerObject(answerObjects, answer.rightAnswerId(), data.questionId()));
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
        // Идентификаторы ответов нужны сразу: следующая подсказка ссылается на них,
        // чтобы перенести уже данные ответы в своё взаимодействие.
        interactionRepository.flush();

        return questionInteractionMapper.map(interaction);
    }

    @Transactional
    public void grade(long interactionId, float grade) {
        var interaction = interactionRepository.findById(interactionId).orElseThrow(
                () -> new NoSuchElementException("Interaction " + interactionId + " not found"));
        Strict.required(interaction.getFeedback(), "feedback", "interaction " + interactionId)
                .setGrade(grade);
        interactionRepository.save(interaction);
    }

    private @NotNull ResponseEntity existingResponse(long responseId) {
        return responseRepository.findById(responseId).orElseThrow(
                () -> new NoSuchElementException("Response " + responseId + " not found"));
    }

    private @NotNull QuestionEntity findQuestion(long questionId) {
        return questionRepository.findByIdFetchingAnswerObjects(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));
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
