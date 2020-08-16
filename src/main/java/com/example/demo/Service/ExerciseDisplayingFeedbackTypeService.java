package com.example.demo.Service;

import com.example.demo.models.repository.ExerciseDisplayingFeedbackTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseDisplayingFeedbackTypeService {
    private ExerciseDisplayingFeedbackTypeRepository exerciseDisplayingFeedbackTypeRepository;

    @Autowired
    public ExerciseDisplayingFeedbackTypeService(ExerciseDisplayingFeedbackTypeRepository exerciseDisplayingFeedbackTypeRepository) {
        this.exerciseDisplayingFeedbackTypeRepository = exerciseDisplayingFeedbackTypeRepository;
    }
}
