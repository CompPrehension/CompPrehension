package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;

import java.util.ArrayList;
import java.util.List;

public abstract class Domain {

    protected com.example.demo.models.entities.Domain domain;

    public abstract void update();

    public Domain(com.example.demo.models.entities.Domain domain) {
        this.domain = domain;
    }

    public abstract ExerciseForm getExerciseForm();
    
    public abstract Exercise processExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
    public abstract ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType);
    
}
