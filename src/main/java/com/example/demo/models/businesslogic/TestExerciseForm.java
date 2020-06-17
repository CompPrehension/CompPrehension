package com.example.demo.models.businesslogic;

import com.example.demo.Service.ConceptService;
import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.Domain;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.ExerciseConcept;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestExerciseForm extends ExerciseForm {

    
    private Domain domain; 
    
    public TestExerciseForm(Domain domain) {
        
        this.domain = domain;
        super.allConcepts.addAll(domain.getConcepts());
    }

    @Override
    public Map<String, String> validate() {
        
        Map<String, String> errors = new HashMap<>();
        for (Concept c : deniedConcepts) {
            
            if (!allConcepts.contains(c)) {
                
                errors.put(c.getName(), "Такого концепта нет в этой предметной" +
                        "области");
            }
        }
        
        return null;
    }

    @Override
    public void fillForm(Exercise exercise) {
        
        for (ExerciseConcept ec: exercise.getExerciseConcepts()) {
            
            if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {
                deniedConcepts.add(ec.getConcept());
            }
        }
    }    
}
