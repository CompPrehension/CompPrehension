package org.vstu.compprehension.Service;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseStageDto;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
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
    private final DomainFactory domainFactory;

    @Autowired
    public ExerciseService(
            DomainRepository domainRepository,
            ExerciseRepository exerciseRepository,
            DomainFactory domainFactory
    ) {
        this.domainRepository = domainRepository;
        this.exerciseRepository = exerciseRepository;
        this.domainFactory = domainFactory;
    }

    public ExerciseEntity getExercise(long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(()->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found"));
    }

    public ExerciseEntity createExercise(@NotNull String name,
                                         @NotNull String domainId,
                                         @NotNull String strategyId
    ) {
        var domainEntity = domainRepository.findById(domainId)
                .orElseThrow();
        var domain = domainFactory.getDomain(domainEntity.getName());
        var backendId = domain.getBackendId();

        var exercise = new ExerciseEntity();
        exercise.setName(name);
        exercise.setDomain(domainEntity);
        exercise.setComplexity(0.5f);
        exercise.setBackendId(backendId);
        exercise.setStrategyId(strategyId);
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
        exercise.setComplexity(card.getComplexity());
        exercise.setStages(card.getStages()
                .stream().map(s -> new ExerciseStageEntity(s.getNumberOfQuestions(), s.getLaws(), s.getConcepts()))
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
                        .stream().map(s -> new ExerciseStageDto(s.getNumberOfQuestions(), s.getLaws(), s.getConcepts()))
                        .collect(Collectors.toList()))
                .complexity(exercise.getComplexity())
                .options(exercise.getOptions())
                .tags(exercise.getTags())
                .build();
    }
}
