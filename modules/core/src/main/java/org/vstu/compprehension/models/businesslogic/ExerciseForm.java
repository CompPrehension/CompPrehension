package org.vstu.compprehension.models.businesslogic;

import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ExerciseForm {
    
    protected List<Concept> allConcepts = new ArrayList<>();
    
    protected List<Concept> deniedConcepts = new ArrayList<>();
    
    public abstract Map<String, String> validate();
    
    public abstract void fillForm(ExerciseEntity exercise);

    public List<Concept> getAllConcepts() {
        return allConcepts;
    }

    public void setAllConcepts(List<Concept> allConcepts) {
        this.allConcepts = allConcepts;
    }

    public List<Concept> getDeniedConcepts() {
        return deniedConcepts;
    }

    public void setDeniedConcepts(List<Concept> deniedConcepts) {
        this.deniedConcepts = deniedConcepts;
    }
}
