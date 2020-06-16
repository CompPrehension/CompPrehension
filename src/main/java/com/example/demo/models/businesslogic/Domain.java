package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;

import java.util.List;

public abstract class Domain {
    
    public abstract ExerciseForm getExerciseForm();
    
    public abstract Exercise ProcessExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
    public abstract HyperText makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType);
    
}
