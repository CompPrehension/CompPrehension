package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.repository.UserActionRepository;
import org.vstu.compprehension.models.entities.UserActionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserActionService {
    private UserActionRepository userActionRepository;

    @Autowired
    public UserActionService(UserActionRepository userActionRepository) {
        this.userActionRepository = userActionRepository;
    }

    public void saveUserAction(UserActionEntity userAction) {
        
        userActionRepository.save(userAction);
    }
}
