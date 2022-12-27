package org.vstu.compprehension.Service;

import org.vstu.compprehension.models.entities.UserEntity;

public interface UserService {
    UserEntity getCurrentUser() throws Exception;
}
