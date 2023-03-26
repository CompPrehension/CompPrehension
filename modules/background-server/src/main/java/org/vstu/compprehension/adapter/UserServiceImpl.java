package org.vstu.compprehension.adapter;

import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.UserEntity;

@Component
public class UserServiceImpl implements UserService {

    @Override
    public UserEntity getCurrentUser() throws Exception {
        throw new Exception("No user");
    }
}
