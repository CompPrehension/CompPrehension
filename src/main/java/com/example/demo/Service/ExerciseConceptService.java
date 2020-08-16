package com.example.demo.Service;

import com.example.demo.models.repository.ExerciseConceptRepository;
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
