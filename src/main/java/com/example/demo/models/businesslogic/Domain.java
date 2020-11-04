package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.Law;
import com.example.demo.models.entities.Mistake;
import com.example.demo.utils.HyperText;

import java.util.ArrayList;
import java.util.List;

public abstract class Domain {

    protected DomainEntity domainEntity;

    public abstract void update();

    public String getName() {
        return domainEntity.getName();
    }

    public List<Law> getLaws() {
        return domainEntity.getLaws();
    }

    public Domain(DomainEntity domainEntity) {
        this.domainEntity = domainEntity;
    }

    public abstract ExerciseForm getExerciseForm();
    
    public abstract Exercise processExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
    public abstract ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType);
    
}
