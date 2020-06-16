package com.example.demo.Service;

import com.example.demo.models.Dao.UserActionExerciseDao;
import com.example.demo.models.entities.UserActionExercise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActionExerciseService {
    private UserActionExerciseDao userActionExerciseDao;

    @Autowired
    public UserActionExerciseService(UserActionExerciseDao userActionExerciseDao) {
        this.userActionExerciseDao = userActionExerciseDao;
    }
    
    public void saveUserActionExercise(UserActionExercise userActionExercise) {
        
        userActionExerciseDao.save(userActionExercise);
    }
}
