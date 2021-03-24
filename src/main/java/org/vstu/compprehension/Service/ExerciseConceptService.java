package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.repository.ExerciseConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseConceptService {
    private ExerciseConceptRepository exerciseConceptRepository;

    @Autowired
    public ExerciseConceptService(ExerciseConceptRepository exerciseConceptRepository) {
        this.exerciseConceptRepository = exerciseConceptRepository;
    }
}
