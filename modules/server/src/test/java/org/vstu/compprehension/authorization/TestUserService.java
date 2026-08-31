package org.vstu.compprehension.authorization;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.UserEntity;
import org.vstu.compprehension.models.repository.UserRepository;

import java.util.NoSuchElementException;

@Primary
@Component
@Profile("test")
public class TestUserService implements UserService {

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
    public UserEntity getCurrentUser() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("Текущий пользователь не задан: вызовите actingAs(...)");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Нет пользователя с id " + userId));
    }

    @Override
    public void setLanguage(Language language) {
        var user = getCurrentUser();
        user.setPreferred_language(language);
        userRepository.save(user);
    }
}
