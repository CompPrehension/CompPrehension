package com.example.demo.Service;

import com.example.demo.models.repository.UserActionExerciseRepository;
import com.example.demo.models.entities.UserActionExerciseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActionExerciseService {
    private UserActionExerciseRepository userActionExerciseRepository;

    @Autowired
    public UserActionExerciseService(UserActionExerciseRepository userActionExerciseRepository) {
        this.userActionExerciseRepository = userActionExerciseRepository;
    }
    
    public void saveUserActionExercise(UserActionExerciseEntity userActionExercise) {
        
        userActionExerciseRepository.save(userActionExercise);
    }
}
