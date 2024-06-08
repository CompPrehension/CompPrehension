package org.vstu.compprehension.Service;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseStageDto;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.CourseRepository;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ExerciseService {
    private final DomainRepository domainRepository;
    private final ExerciseRepository exerciseRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public ExerciseService(DomainRepository domainRepository,
                           ExerciseRepository exerciseRepository, CourseRepository courseRepository) {
        this.domainRepository = domainRepository;
        this.exerciseRepository = exerciseRepository;
        this.courseRepository = courseRepository;
    }

    public ExerciseEntity getExercise(long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(()->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found"));
    }

    public ExerciseEntity createExercise(@NotNull String name,
                                         @NotNull String domainId,
                                         @NotNull String strategyId,
                                         long courseId
    ) {
        var domain = domainRepository.findById(domainId)
                .orElseThrow();
        var backendId = JenaBackend.BackendId;
        var course = courseRepository.findById(courseId)
                .orElseThrow();

        var exercise = new ExerciseEntity();
        exercise.setName(name);
        exercise.setDomain(domain);
        exercise.setComplexity(0.5f);
        exercise.setBackendId(backendId);
        exercise.setStrategyId(strategyId);
        exercise.setCourse(course);
        exercise.setOptions(ExerciseOptionsEntity.builder()
                .forceNewAttemptCreationEnabled(true)
                .correctAnswerGenerationEnabled(true)
                .newQuestionGenerationEnabled(true)
                .supplementaryQuestionsEnabled(true)
                .preferDecisionTreeBasedSupplementaryEnabled(false)
                .build());
        exercise.setStages(new ArrayList<>(List.of(new ExerciseStageEntity(5, new ArrayList<>(), new ArrayList<>()))));
        exercise.setTags("");
        exerciseRepository.save(exercise);
        return exercise;
    }

    public void saveExerciseCard(ExerciseCardDto card) {
        var exercise = exerciseRepository.findById(card.getId()).orElseThrow(() ->
                new NoSuchElementException("Exercise with id: " + card.getId() + " not found"));
        var domain = domainRepository.findById(card.getDomainId())
                .orElseThrow();
        var course = courseRepository.findById(card.getCourseId())
                .orElseThrow();

        exercise.setName(card.getName());
        exercise.setDomain(domain);
        exercise.setCourse(course);
        exercise.setStrategyId(card.getStrategyId());
        exercise.setBackendId(card.getBackendId());
        exercise.setTags(String.join(", ", card.getTags()));
        exercise.setOptions(card.getOptions());
        exercise.setComplexity(card.getComplexity());
        exercise.setStages(card.getStages()
                .stream().map(s -> new ExerciseStageEntity(s.getNumberOfQuestions(), s.getLaws(), s.getConcepts()))
                .collect(Collectors.toList()));

        exerciseRepository.save(exercise);
    }

    public ExerciseCardDto getExerciseCard(long exerciseId) {
        var exercise = exerciseRepository.findById(exerciseId).orElseThrow(() ->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not found"));

        var course = exercise.getCourse();
        if (course == null) {
            exercise.setCourse(courseRepository.findById(1L).orElseThrow());
            exercise = exerciseRepository.save(exercise);
        }

        return ExerciseCardDto.builder()
                .id(exercise.getId())
                .name(exercise.getName())
                .domainId(exercise.getDomain().getName())
                .courseId(exercise.getCourse().getId())
                .strategyId(exercise.getStrategyId())
                .backendId(exercise.getBackendId())
                .stages(exercise.getStages()
                        .stream().map(s -> new ExerciseStageDto(s.getNumberOfQuestions(), s.getLaws(), s.getConcepts()))
                        .collect(Collectors.toList()))
                .complexity(exercise.getComplexity())
                .options(exercise.getOptions())
                .tags(exercise.getTags())
                .build();
    }
}
