package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.repository.UserActionExerciseRepository;
import org.vstu.compprehension.models.entities.UserActionExerciseEntity;
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
