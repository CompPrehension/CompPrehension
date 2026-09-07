package org.vstu.compprehension.adapter;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.services.UserService;
import org.vstu.compprehension.data.enums.Language;
import org.vstu.compprehension.data.user.CurrentUserData;

@Component
public class UserServiceImpl implements UserService {
    @SneakyThrows
    @Override
    public CurrentUserData getCurrentUser() {
        throw new Exception("No user");
    }

    @SneakyThrows
    @Override
    public void setLanguage(Language language) {
        throw new Exception("No user");
    }
}
