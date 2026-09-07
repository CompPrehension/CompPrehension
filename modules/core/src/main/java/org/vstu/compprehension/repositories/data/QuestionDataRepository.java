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

    @Transactional(readOnly = true)
    public @NotNull QuestionData findById(long questionId) {
        var question = questionRepository.findByIdFetchingMetadata(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question " + questionId + " not found"));

        questionRepository.findByIdFetchingAnswerObjects(questionId);
        violationRepository.findAllByQuestionIdFetchingTemplates(questionId);
        var interactions = interactionRepository.findAllByQuestionIdFetchingViolations(questionId);
        interactionRepository.findAllByQuestionIdFetchingResponses(questionId);
        interactionRepository.findAllByQuestionIdFetchingCorrectLaws(questionId);

        return toData(question, interactions);
    }

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

    private static @NotNull QuestionEntity toNewEntity(
            @NotNull QuestionData data,
            @Nullable QuestionMetadataEntity metadata) {
        var entity = new QuestionEntity();
        entity.setAnswerObjects(new ArrayList<>());
        entity.setInteractions(new ArrayList<>());
        applyToEntity(data, entity, metadata);
        return entity;
    }

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
        data.setMetadata(QuestionMetadataMapping.toData(entity.getMetadata()));
        data.setStatementFacts(copyFacts(entity.getStatementFacts()));
        data.setSolutionFacts(copyFacts(entity.getSolutionFacts()));
        data.setAnswerObjects(entity.getAnswerObjects().stream()
                .map(QuestionDataRepository::toData)
                .collect(Collectors.toCollection(ArrayList::new)));

        var interactionsData = interactions.stream()
                .sorted(Comparator.comparing(InteractionEntity::getId))
                .map(QuestionDataRepository::toData)
                .collect(Collectors.toCollection(ArrayList::new));
        interactionsData.forEach(interaction -> interaction.setQuestion(data));
        data.setInteractions(interactionsData);
        return data;
    }

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
