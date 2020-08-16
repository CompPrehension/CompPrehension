package com.example.demo.Service;

import com.example.demo.models.repository.ExerciseDisplayingFeedbackTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseQuestionTypeService {
    private ExerciseDisplayingFeedbackTypeRepository exerciseDisplayingFeedbackTypeRepository;

    @Autowired
    public ExerciseQuestionTypeService(ExerciseDisplayingFeedbackTypeRepository exerciseDisplayingFeedbackTypeRepository) {
        this.exerciseDisplayingFeedbackTypeRepository = exerciseDisplayingFeedbackTypeRepository;
    }
}
