package com.example.demo.models.businesslogic;

import com.example.demo.models.entities.Exercise;

import java.util.HashMap;
import java.util.Map;

public abstract class ExerciseForm {
    
    public Map<String, String> validate() {
        
        return new HashMap<>();
    }
    
    public void fillForm(Exercise exercise) {
        
    }
}
