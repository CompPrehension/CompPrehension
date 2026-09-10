package org.vstu.compprehension.authorization;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.data.user.UserData;
import org.vstu.compprehension.services.UserDataService;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.repositories.entity.UserRepository;

import java.util.NoSuchElementException;

@Primary
@Component
@Profile("test")
public class TestUserService implements UserDataService {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private final UserRepository userRepository;

    public TestUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static void actAs(long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static void reset() {
        CURRENT_USER_ID.remove();
    }

    @Override
    public UserData getCurrentUser() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("Текущий пользователь не задан: вызовите actingAs(...)");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Нет пользователя с id " + userId));
        return new UserData(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPreferred_language());
    }

    @Override
    public void setLanguage(Language language) {
        var user = userRepository.findById(getCurrentUser().id())
                .orElseThrow(() -> new NoSuchElementException("Нет текущего пользователя"));
        user.setPreferred_language(language);
        userRepository.save(user);
    }
}
