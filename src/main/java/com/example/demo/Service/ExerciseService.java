package com.example.demo.Service;

import com.example.demo.models.Dao.ExerciseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseService {
    private ExerciseDao exerciseDao;

    @Autowired
    public ExerciseService(ExerciseDao exerciseDao) {
        this.exerciseDao = exerciseDao;
    }
}
