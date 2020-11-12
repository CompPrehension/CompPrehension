package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.utils.HyperText;

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

    public List<Concept> getConcepts() {
        return concepts;
    }

    public Law getLaw(String name) throws Exception {
        for (Law law : laws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        throw new Exception();
    }

    public Concept getConcept(String name) throws Exception {
        for (Concept concept : concepts) {
            if (name.equals(concept.getName())) {
                return concept;
            }
        }
        throw new Exception();
    }

    public Domain(DomainEntity domainEntity) {
        this.domainEntity = domainEntity;
    }

    public abstract ExerciseForm getExerciseForm();
    
    public abstract Exercise processExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
    public abstract ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType);
    
}
