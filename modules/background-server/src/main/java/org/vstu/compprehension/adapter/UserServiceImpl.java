package org.vstu.compprehension.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.mappers.Mapper;
import org.vstu.compprehension.repositories.data.UserDataRepository;
import org.vstu.compprehension.services.UserDataService;

@Component
@RequiredArgsConstructor
public class UserServiceImpl implements UserDataService {
    private static final long ADMIN_USER_ID = 1;

    private final UserDataRepository users;
    private final Mapper<UserAccountData, UserData> currentUserMapper;

    @Override
    public UserData getCurrentUser() {
        return currentUserMapper.map(users.getById(ADMIN_USER_ID));
    }

    @Override
    public void setLanguage(Language language) {
        // do nothing
    }
}
