package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.utils.HyperText;

import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

public abstract class Domain {
    protected List<Law> laws;
    protected List<Concept> concepts;

    protected DomainEntity domainEntity;

    public abstract void update();

    public String getName() {
        return domainEntity.getName();
    }

    public List<Law> getLaws() {
        return laws;
    }

    public Domain(DomainEntity domainEntity) {
        this.domainEntity = domainEntity;
    }

    public abstract ExerciseForm getExerciseForm();
    
    public abstract Exercise processExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
    public abstract ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType);
    
}
