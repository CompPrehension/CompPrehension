package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.entities.EnumData.RoleInExercise;
import com.example.demo.models.entities.Exercise;
import com.example.demo.models.entities.ExerciseConcept;

import java.util.HashMap;
import java.util.Map;

public class TestExerciseForm extends ExerciseForm {

    
    private DomainEntity domainEntity;
    
    public TestExerciseForm(DomainEntity domainEntity) {
        
        this.domainEntity = domainEntity;
        //super.allConcepts.addAll(domainEntity.getConcepts());
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
                //deniedConcepts.add(ec.getConcept());
            }
        }
    }    
}
