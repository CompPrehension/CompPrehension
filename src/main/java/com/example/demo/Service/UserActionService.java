package com.example.demo.Service;

import com.example.demo.models.repository.UserActionRepository;
import com.example.demo.models.entities.UserAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActionService {
    private UserActionRepository userActionRepository;

    @Autowired
    public UserActionService(UserActionRepository userActionRepository) {
        this.userActionRepository = userActionRepository;
    }

    public void saveUserAction(UserAction userAction) {
        
        userActionRepository.save(userAction);
    }
}
