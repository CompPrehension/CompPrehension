package com.example.demo.Service;

import com.example.demo.models.Dao.ExerciseDisplayingFeedbackTypeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseDisplayingFeedbackTypeService {
    private ExerciseDisplayingFeedbackTypeDao exerciseDisplayingFeedbackTypeDao;

    @Autowired
    public ExerciseDisplayingFeedbackTypeService(ExerciseDisplayingFeedbackTypeDao exerciseDisplayingFeedbackTypeDao) {
        this.exerciseDisplayingFeedbackTypeDao = exerciseDisplayingFeedbackTypeDao;
    }
}
