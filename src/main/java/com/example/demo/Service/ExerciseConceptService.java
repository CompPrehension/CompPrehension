package com.example.demo.Service;

import com.example.demo.models.Dao.ExerciseConceptDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseConceptService {
    private ExerciseConceptDao exerciseConceptDao;

    @Autowired
    public ExerciseConceptService(ExerciseConceptDao exerciseConceptDao) {
        this.exerciseConceptDao = exerciseConceptDao;
    }
}
