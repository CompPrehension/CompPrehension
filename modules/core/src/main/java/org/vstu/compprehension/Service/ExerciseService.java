package org.vstu.compprehension.Service;


import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class ExerciseService {
    @Autowired
    private ExerciseRepository exerciseRepository;

    public ExerciseEntity getExercise(long exerciseId) {
        return exerciseRepository.findById(exerciseId).orElseThrow(()->
                new NoSuchElementException("Exercise with id: " + exerciseId + " not Found"));
    }
}
