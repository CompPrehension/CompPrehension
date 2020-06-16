package com.example.demo.Service;

import com.example.demo.models.Dao.UserActionDao;
import com.example.demo.models.entities.UserAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActionService {
    private UserActionDao userActionDao;

    @Autowired
    public UserActionService(UserActionDao userActionDao) {
        this.userActionDao = userActionDao;
    }

    public void saveUserAction(UserAction userAction) {
        
        userActionDao.save(userAction);
    }
}
