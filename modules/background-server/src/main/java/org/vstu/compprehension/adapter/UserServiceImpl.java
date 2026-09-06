package org.vstu.compprehension.adapter;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.data.CurrentUserData;

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
