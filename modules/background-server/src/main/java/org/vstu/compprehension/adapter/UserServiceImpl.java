package org.vstu.compprehension.adapter;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.services.UserDataService;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.data.user.UserData;

@Component
public class UserServiceImpl implements UserDataService {
    @SneakyThrows
    @Override
    public UserData getCurrentUser() {
        throw new Exception("No user");
    }

    @SneakyThrows
    @Override
    public void setLanguage(Language language) {
        throw new Exception("No user");
    }
}
