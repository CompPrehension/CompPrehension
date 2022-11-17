package org.vstu.compprehension.Service;


import org.vstu.compprehension.dto.ExerciseCardDto;
import org.vstu.compprehension.dto.ExerciseLConceptDto;
import org.vstu.compprehension.dto.ExerciseLawDto;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ExerciseService {
    @Autowired
    private ExerciseRepository exerciseRepository;

    public ExerciseEntity getExercise(long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(()->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found"));
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
                .concepts(exercise.getExerciseConcepts().stream().map(l -> ExerciseLConceptDto.builder()
                        .name(l.getConceptName())
                        .kind(l.getRoleInExercise())
                        .build()).collect(Collectors.toList()))
                .complexity(exercise.getComplexity())
                .answerLength(exercise.getTimeLimit())
                .build();
    }
}
