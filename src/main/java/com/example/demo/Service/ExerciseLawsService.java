package com.example.demo.Service;

import com.example.demo.models.repository.ExerciseLawsRepository;
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
