package org.vstu.compprehension.Service;


import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.entities.ExerciseConceptEntity;
import org.vstu.compprehension.models.entities.ExerciseLawsEntity;
import org.vstu.compprehension.models.entities.ExerciseOptionsEntity;
import org.vstu.compprehension.models.repository.DomainRepository;
import org.vstu.compprehension.models.repository.ExerciseConceptRepository;
import org.vstu.compprehension.models.repository.ExerciseLawsRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ExerciseService {
    private final DomainRepository domainRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseLawsRepository exerciseLawsRepository;
    private final ExerciseConceptRepository exerciseConceptRepository;

    @Autowired
    public ExerciseService(DomainRepository domainRepository,
                           ExerciseRepository exerciseRepository,
                           ExerciseLawsRepository exerciseLawsRepository,
                           ExerciseConceptRepository exerciseConceptRepository) {
        this.domainRepository = domainRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseLawsRepository = exerciseLawsRepository;
        this.exerciseConceptRepository = exerciseConceptRepository;
    }

    public ExerciseEntity getExercise(long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(()->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found"));
    }

    public ExerciseEntity createExercise(@NotNull String name,
                                         @NotNull String domainId,
                                         @NotNull String strategyId
    ) {
        var domain = domainRepository.findById(domainId)
                .orElseThrow();
        var backendId = JenaBackend.BackendId;

        var exercise = new ExerciseEntity();
        exercise.setName(name);
        exercise.setDomain(domain);
        exercise.setComplexity(0.5f);
        exercise.setTimeLimit(0.5f);
        exercise.setBackendId(backendId);
        exercise.setStrategyId(strategyId);
        exercise.setNumberOfQuestions(10);
        exercise.setOptions(ExerciseOptionsEntity.builder()
                .forceNewAttemptCreationEnabled(true)
                .correctAnswerGenerationEnabled(true)
                .newQuestionGenerationEnabled(true)
                .supplementaryQuestionsEnabled(true)
                .build());
        exercise.setTags("");
        exerciseRepository.save(exercise);
        return exercise;
    }

    public void saveExerciseCard(ExerciseCardDto card) {
        var exercise = exerciseRepository.findById(card.getId()).orElseThrow(() ->
                new NoSuchElementException("Exercise with id: " + card.getId() + " not found"));

        exercise.setName(card.getName());
        exercise.setStrategyId(card.getStrategyId());
        exercise.setBackendId(card.getBackendId());

        {
            var cardConcepts = new HashMap<String, ExerciseConceptDto>();
            for (var c: card.getConcepts()) {
                cardConcepts.put(c.getName(), c);
            }
            var rawExerciseConcepts = exercise.getExerciseConcepts();
            var exerciseConcepts = new HashMap<String, ExerciseConceptEntity>();
            for (var c: rawExerciseConcepts) {
                exerciseConcepts.put(c.getConceptName(), c);
            }

            var conceptsToAdd = cardConcepts.keySet()
                    .stream().filter(c -> !exerciseConcepts.containsKey(c))
                    .map(cardConcepts::get)
                    .collect(Collectors.toList());
            for (var c: conceptsToAdd) {
                var ce = new ExerciseConceptEntity();
                ce.setConceptName(c.getName());
                ce.setRoleInExercise(c.getKind());
                ce.setExercise(exercise);
                rawExerciseConcepts.add(ce);
            }

            var conceptsToRemove = exerciseConcepts.keySet()
                    .stream().filter(c -> !cardConcepts.containsKey(c))
                    .map(exerciseConcepts::get)
                    .collect(Collectors.toList());
            rawExerciseConcepts.removeAll(conceptsToRemove);
            exerciseConceptRepository.deleteAll(conceptsToRemove);

            var conceptsToUpdate = exerciseConcepts.keySet()
                    .stream().filter(cardConcepts::containsKey)
                    .map(exerciseConcepts::get)
                    .collect(Collectors.toList());
            for (var c: conceptsToUpdate) {
                var update = cardConcepts.get(c.getConceptName());
                c.setRoleInExercise(update.getKind());
            }
            exerciseConceptRepository.saveAll(exercise.getExerciseConcepts());
        }

        {
            var cardLaws = new HashMap<String, ExerciseLawDto>();
            for (var l: card.getLaws()) {
                cardLaws.put(l.getName(), l);
            }
            var exerciseLaws = new HashMap<String, ExerciseLawsEntity>();
            for (var l: exercise.getExerciseLaws()) {
                exerciseLaws.put(l.getLawName(), l);
            }

            var lawsToAdd = cardLaws.keySet()
                    .stream().filter(c -> !exerciseLaws.containsKey(c))
                    .map(cardLaws::get)
                    .collect(Collectors.toList());
            for (var l: lawsToAdd) {
                var le = new ExerciseLawsEntity();
                le.setLawName(l.getName());
                le.setRoleInExercise(l.getKind());
                le.setExercise(exercise);
                exercise.getExerciseLaws().add(le);
            }

            var lawsToRemove = exerciseLaws.keySet()
                    .stream().filter(c -> !cardLaws.containsKey(c))
                    .map(exerciseLaws::get)
                    .collect(Collectors.toList());
            exercise.getExerciseLaws().removeAll(lawsToRemove);
            exerciseLawsRepository.deleteAll(lawsToRemove);

            var lawsToUpdate = exerciseLaws.keySet()
                    .stream().filter(cardLaws::containsKey)
                    .map(exerciseLaws::get)
                    .collect(Collectors.toList());
            for (var l: lawsToUpdate) {
                var update = cardLaws.get(l.getLawName());
                l.setRoleInExercise(update.getKind());
            }
            exerciseLawsRepository.saveAll(exercise.getExerciseLaws());
        }

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
                .laws(exercise.getExerciseLaws().stream().map(l -> ExerciseLawDto.builder()
                        .name(l.getLawName())
                        .kind(l.getRoleInExercise())
                        .build()).collect(Collectors.toList()))
                .concepts(exercise.getExerciseConcepts().stream().map(l -> ExerciseConceptDto.builder()
                        .name(l.getConceptName())
                        .kind(l.getRoleInExercise())
                        .build()).collect(Collectors.toList()))
                .complexity(exercise.getComplexity())
                .answerLength(exercise.getTimeLimit())
                .options(exercise.getOptions())
                .tags(exercise.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList()))
                .build();
    }
}
