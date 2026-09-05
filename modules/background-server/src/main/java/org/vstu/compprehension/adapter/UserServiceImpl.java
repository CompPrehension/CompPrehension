package org.vstu.compprehension.adapter;

import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.data.CurrentUserData;

@Component
public class UserServiceImpl implements UserService {
    @Override
    public CurrentUserData getCurrentUser() throws Exception {
        throw new Exception("No user");
    }

    @Override
    public void setLanguage(Language language) throws Exception {
        throw new Exception("No user");
    }
}
