package com.example.demo.Service;

import com.example.demo.models.Dao.ExerciseDisplayingFeedbackTypeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExerciseQuestionTypeService {
    private ExerciseDisplayingFeedbackTypeDao exerciseDisplayingFeedbackTypeDao;

    @Autowired
    public ExerciseQuestionTypeService(ExerciseDisplayingFeedbackTypeDao exerciseDisplayingFeedbackTypeDao) {
        this.exerciseDisplayingFeedbackTypeDao = exerciseDisplayingFeedbackTypeDao;
    }
}
