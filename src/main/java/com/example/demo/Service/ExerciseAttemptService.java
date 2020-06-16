package com.example.demo.Service;

import com.example.demo.models.Dao.ExerciseAttemptDao;
import com.example.demo.models.entities.ExerciseAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseAttemptService {
    private ExerciseAttemptDao exerciseAttemptDao;

    @Autowired
    public ExerciseAttemptService(ExerciseAttemptDao exerciseAttemptDao) {
        this.exerciseAttemptDao = exerciseAttemptDao;
    }
    
    
    public void saveExerciseAttempt(ExerciseAttempt exerciseAttempt) {
        
        exerciseAttemptDao.save(exerciseAttempt);
    }
}
