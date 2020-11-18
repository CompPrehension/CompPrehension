package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.utils.HyperText;

import java.util.ArrayList;
import java.util.List;

public abstract class Domain {
    protected List<PositiveLaw> positiveLaws;
    protected List<NegativeLaw> negativeLaws;
    protected List<Concept> concepts;

    protected DomainEntity domainEntity;

    public abstract void update();

    public String getName() {
        return domainEntity.getName();
    }
    
    public String getVersion() {
        return domainEntity.getVersion();
    }

    public List<PositiveLaw> getPositiveLaws() {
        return positiveLaws;
    }
    public List<NegativeLaw> getNegativeLaws() {
        return negativeLaws;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public PositiveLaw getPositiveLaw(String name) {
        for (PositiveLaw law : positiveLaws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        return null;
    }

    public NegativeLaw getNegativeLaw(String name) {
        for (NegativeLaw law : negativeLaws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        return null;
    }

    public Concept getConcept(String name) {
        for (Concept concept : concepts) {
            if (name.equals(concept.getName())) {
                return concept;
            }
        }
        return null;
    }

    public Domain(DomainEntity domainEntity) {
        this.domainEntity = domainEntity;
    }

    public abstract ExerciseForm getExerciseForm();
    
    public abstract Exercise processExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
    public abstract ArrayList<HyperText> makeExplanation(List<Mistake> mistakes, FeedbackType feedbackType);
    
}
