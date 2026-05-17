package org.vstu.compprehension.Service;


import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseDto;
import org.vstu.compprehension.dto.ExerciseStageDto;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseCourseLinkRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ExerciseService {
    private final DomainRepository domainRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseCourseLinkRepository exerciseCourseLinkRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final DomainFactory domainFactory;
    private final CourseService courseService;
    private final ExerciseCourseLinkReassignExecutor linkReassignExecutor;
    private final ExerciseAttemptReassignExecutor attemptReassignExecutor;

    public ExerciseService(
            DomainRepository domainRepository,
            ExerciseRepository exerciseRepository,
            ExerciseCourseLinkRepository exerciseCourseLinkRepository,
            ExerciseAttemptRepository exerciseAttemptRepository,
            DomainFactory domainFactory,
            CourseService courseService,
            ExerciseCourseLinkReassignExecutor linkReassignExecutor,
            ExerciseAttemptReassignExecutor attemptReassignExecutor
    ) {
        this.domainRepository = domainRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseCourseLinkRepository = exerciseCourseLinkRepository;
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.domainFactory = domainFactory;
        this.courseService = courseService;
        this.linkReassignExecutor = linkReassignExecutor;
        this.attemptReassignExecutor = attemptReassignExecutor;
    }

    public ExerciseEntity getExercise(long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(()->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found"));
    }

    @Transactional
    public ExerciseEntity createExercise(@NotNull String name,
                                         @NotNull String domainId,
                                         @NotNull String strategyId,
                                         @Nullable Long courseId
    ) {
        var domainEntity = domainRepository.findById(domainId)
                .orElseThrow();
        var domain = domainFactory.getDomain(domainEntity.getName());
        var backendId = domain.getBackendId();

        var exercise = new ExerciseEntity();
        exercise.setName(name);
        exercise.setDomain(domainEntity);
        exercise.setBackendId(backendId);
        exercise.setStrategyId(strategyId);
        exercise.setOptions(ExerciseOptionsEntity.builder()
                .forceNewAttemptCreationEnabled(true)
                .correctAnswerGenerationEnabled(true)
                .newQuestionGenerationEnabled(true)
                .supplementaryQuestionsEnabled(true)
                .debugButtonEnabled(false)
                .preferDecisionTreeBasedSupplementaryEnabled(false)
                .build());
        exercise.setStages(new ArrayList<>(List.of(new ExerciseStageEntity(5, 0.5f, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()))));
        exercise.setTags("");
        exercise.setPublic(courseId == null);
        exercise.setGuid(UUID.randomUUID());
        exerciseRepository.save(exercise);

        if (courseId != null) {
            courseService.linkExerciseWithCourseIfMissing(exercise.getId(), courseId);
        }
        return exercise;
    }

    @Transactional
    public ExerciseEntity cloneExercise(long sourceExerciseId, @Nullable Long targetCourseId) {
        var source = exerciseRepository.findById(sourceExerciseId)
                .orElseThrow(() -> new NoSuchElementException("exercise not found"));

        if (!source.isPublic() && targetCourseId != null) {
            var sourceLinks = exerciseCourseLinkRepository.findAllByExerciseId(sourceExerciseId);
            Long sourceCourseId = sourceLinks.size() == 1 ? sourceLinks.get(0).getCourse().getId() : null;
            if (targetCourseId.equals(sourceCourseId)) {
                throw new IllegalStateException("duplicating_in_same_course");
            }
            throw new IllegalStateException("course_to_course_forbidden: copy to pool first, then link");
        }

        var clone = source.clone();
        clone.setPublic(targetCourseId == null);
        exerciseRepository.save(clone);

        if (targetCourseId != null) {
            courseService.linkExerciseWithCourseIfMissing(clone.getId(), targetCourseId);
        }
        return clone;
    }

    @Transactional
    public void deleteExercise(long exerciseId) {
        var exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("exercise not found"));

        if (!exercise.isPublic()) {
            exerciseRepository.delete(exercise);
            return;
        }

        var links = exerciseCourseLinkRepository.findAllByExerciseId(exerciseId);
        if (links.isEmpty()) {
            exerciseRepository.delete(exercise);
            return;
        }

        var clones = new ArrayList<ExerciseEntity>(links.size());
        for (var ignored : links) {
            clones.add(exercise.clone());
        }
        exerciseRepository.saveAll(clones);
        exerciseRepository.flush();

        var courseToCloneId = new HashMap<Long, Long>();
        for (int i = 0; i < links.size(); i++) {
            courseToCloneId.put(links.get(i).getCourse().getId(), clones.get(i).getId());
        }

        linkReassignExecutor.reassign(exerciseId, courseToCloneId);
        attemptReassignExecutor.reassign(exerciseId, courseToCloneId);
        exerciseRepository.delete(exercise);
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getCourseExercises(long courseId) {
        return exerciseRepository.findAllByCourseId(courseId).stream()
                .map(e -> new ExerciseDto(e.getId(), e.getName(), e.isPublic(), e.getGuid()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getPublicExercises() {
        return exerciseRepository.findAllByIsPublicTrue().stream()
                .map(e -> new ExerciseDto(e.getId(), e.getName(), e.isPublic(), e.getGuid()))
                .collect(Collectors.toList());
    }

    public void saveExerciseCard(ExerciseCardDto card) {
        var exercise = exerciseRepository.findById(card.getId()).orElseThrow(() ->
                new NoSuchElementException("Exercise with id: " + card.getId() + " not found"));
        var domainEntity = domainRepository.findById(card.getDomainId())
                .orElseThrow();
        var domain = domainFactory.getDomain(domainEntity.getName());
        var backendId = domain.getBackendId();

        exercise.setName(card.getName());
        exercise.setDomain(domainEntity);
        exercise.setStrategyId(card.getStrategyId());
        exercise.setBackendId(backendId);
        exercise.setTags(String.join(", ", card.getTags()));
        exercise.setOptions(card.getOptions());
        exercise.setStages(card.getStages()
                .stream().map(s -> new ExerciseStageEntity(s.getNumberOfQuestions(), s.getComplexity(), s.getLaws(), s.getConcepts(), s.getSkills()))
                .collect(Collectors.toList()));

        exerciseRepository.save(exercise);
    }

    public ExerciseCardDto getExerciseCard(long exerciseId) {
        var exercise = exerciseRepository.findById(exerciseId).orElseThrow(() ->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not found"));

        return ExerciseCardDto.builder()
                .id(exercise.getId())
                .name(exercise.getName())
                .domainId(exercise.getDomain().getName())
                .strategyId(exercise.getStrategyId())
                .backendId(exercise.getBackendId())
                .stages(exercise.getStages()
                        .stream().map(s -> new ExerciseStageDto(
                                s.getNumberOfQuestions(), s.getComplexity(), s.getLaws(), s.getConcepts(), s.getSkills()))
                        .collect(Collectors.toList()))
                .options(exercise.getOptions())
                .tags(exercise.getTags())
                .isPublic(exercise.isPublic())
                .modelId(exercise.getGuid())
                .build();
    }
}
