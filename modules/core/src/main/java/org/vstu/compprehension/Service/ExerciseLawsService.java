package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.repository.ExerciseLawsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseLawsService {
    private ExerciseLawsRepository exerciseLawsRepository;

    @Autowired
    public ExerciseLawsService(ExerciseLawsRepository exerciseLawsRepository) {
        this.exerciseLawsRepository = exerciseLawsRepository;
    }
}
