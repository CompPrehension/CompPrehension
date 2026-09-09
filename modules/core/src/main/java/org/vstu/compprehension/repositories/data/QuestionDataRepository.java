package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionRequestLogData;
import org.vstu.compprehension.entities.AnswerObjectEntity;
import org.vstu.compprehension.entities.InteractionEntity;
import org.vstu.compprehension.entities.QuestionEntity;
import org.vstu.compprehension.entities.QuestionMetadataEntity;
import org.vstu.compprehension.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.entity.AnswerObjectRepository;
import org.vstu.compprehension.repositories.entity.DomainRepository;
import org.vstu.compprehension.repositories.entity.ExerciseAttemptRepository;
import org.vstu.compprehension.repositories.entity.InteractionRepository;
import org.vstu.compprehension.repositories.entity.QuestionMetadataRepository;
import org.vstu.compprehension.repositories.entity.QuestionRepository;
import org.vstu.compprehension.repositories.entity.QuestionRequestLogRepository;
import org.vstu.compprehension.repositories.entity.ViolationRepository;
import org.vstu.compprehension.repositories.mappers.QuestionEntityMapper;
import org.vstu.compprehension.repositories.mappers.QuestionMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final QuestionMapper questionMapper;
    private final QuestionEntityMapper questionEntityMapper;
    private final Mapper<QuestionRequestLogData, QuestionRequestLogEntity> questionRequestLogEntityMapper;

    @Transactional(readOnly = true)
    public @NotNull QuestionData findById(long questionId) {
        var question = questionRepository.findByIdFetchingMetadata(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));

        questionRepository.findByIdFetchingAnswerObjects(questionId);
        violationRepository.findAllByQuestionIdFetchingTemplates(questionId);
        var interactions = interactionRepository.findAllByQuestionIdFetchingViolations(questionId);
        interactionRepository.findAllByQuestionIdFetchingResponses(questionId);
        interactionRepository.findAllByQuestionIdFetchingCorrectLaws(questionId);

        return questionMapper.map(question, interactions);
    }

    @Transactional(readOnly = true)
    public @NotNull String getDomainName(long questionId) {
        return questionRepository.findDomainName(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findOwnerUserId(long questionId) {
        return questionRepository.findOwnerUserId(questionId);
    }

    @Transactional
    public @NotNull QuestionData save(@NotNull QuestionData data,
                                      @Nullable QuestionRequestLogData questionRequestLog,
                                      @Nullable Long exerciseAttemptId) {
        var content = data.getContent();

        // Метаданные приходят из банка заданий и уже существуют, поэтому берутся
        // ссылкой по идентификатору, без запроса.
        var metadata = content.getMetadata() == null || content.getMetadata().getId() == null
                ? null
                : questionMetadataRepository.getReferenceById(content.getMetadata().getId());

        // Идентификатор есть только у вопросов, поднятых из БД: по нему и решается,
        // обновлять существующую строку или заводить новую.
        var entity = data.getId() == null
                ? questionEntityMapper.map(data, metadata)
                : questionRepository.findById(data.getId())
                        .orElseGet(() -> questionEntityMapper.map(data, metadata));
        if (data.getId() != null) {
            questionEntityMapper.apply(data, metadata, entity);
        }

        if (questionRequestLog != null) {
            entity.setQuestionRequestLog(questionRequestLogRepository.save(
                    questionRequestLogEntityMapper.map(questionRequestLog)));
        }
        if (exerciseAttemptId != null) {
            entity.setExerciseAttempt(exerciseAttemptRepository.getReferenceById(exerciseAttemptId));
        }
        if (entity.getDomainEntity() == null) {
            entity.setDomainEntity(domainRepository.getReferenceById(content.getDomainId()));
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

        return questionMapper.map(data, entity);
    }
}
