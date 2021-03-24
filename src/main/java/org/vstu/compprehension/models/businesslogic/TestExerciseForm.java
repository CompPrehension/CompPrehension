package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.models.entities.ExerciseEntity;
import org.vstu.compprehension.models.entities.ExerciseConceptEntity;

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
    public void fillForm(ExerciseEntity exercise) {
        
        for (ExerciseConceptEntity ec: exercise.getExerciseConcepts()) {
            
            if (ec.getRoleInExercise() == RoleInExercise.FORBIDDEN) {
                //deniedConcepts.add(ec.getConcept());
            }
        }
    }    
}
