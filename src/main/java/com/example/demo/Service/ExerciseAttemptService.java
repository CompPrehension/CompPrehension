package com.example.demo.Service;

import com.example.demo.models.repository.ExerciseAttemptRepository;
import com.example.demo.models.entities.ExerciseAttemptEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseAttemptService {
    private ExerciseAttemptRepository exerciseAttemptRepository;

    @Autowired
    public ExerciseAttemptService(ExerciseAttemptRepository exerciseAttemptRepository) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
    }
    
    
    public void saveExerciseAttempt(ExerciseAttemptEntity exerciseAttempt) {
        
        exerciseAttemptRepository.save(exerciseAttempt);
    }
}
