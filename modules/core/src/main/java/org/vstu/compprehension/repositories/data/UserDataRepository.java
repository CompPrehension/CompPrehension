package org.vstu.compprehension.repositories.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.data.user.UserAccountData;
import org.vstu.compprehension.data.user.UserAccountUpdateData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.entities.UserEntity;
import org.vstu.compprehension.repositories.entity.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserDataRepository {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public @NotNull List<Long> findAllIds() {
        return userRepository.findAllIds();
    }

    @Transactional(readOnly = true)
    public @NotNull Optional<UserAccountData> findByEmail(@NotNull String email) {
        return userRepository.findFirstByEmailOrderByIdAsc(email).map(UserDataRepository::toData);
    }

    @Transactional
    public @NotNull UserAccountData save(@NotNull UserAccountUpdateData update) {
        var entity = userRepository.findFirstByEmailOrderByIdAsc(update.email())
                .orElseGet(UserEntity::new);
        entity.setEmail(update.email());
        entity.setLogin(update.email());
        entity.setFirstName(update.fullName());
        entity.setPassword(null);
        entity.setPreferred_language(update.language());
        entity.setExternalId(update.externalId());
        entity.setExternalUserId(update.externalUserId());
        return toData(userRepository.save(entity));
    }

    @Transactional
    public void setLanguage(@NotNull String email, @NotNull Language language) {
        var entity = userRepository.findFirstByEmailOrderByIdAsc(email)
                .orElseThrow(() -> new NoSuchElementException("User " + email + " not found"));
        entity.setPreferred_language(language);
        userRepository.save(entity);
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull UserAccountData toData(@NotNull UserEntity entity) {
        long id = Strict.required(entity.getId(), "id", "user");
        String owner = "user " + id;
        return new UserAccountData(
                id,
                entity.getFirstName(),
                entity.getLastName(),
                Strict.required(entity.getEmail(), "email", owner),
                Strict.required(entity.getPreferred_language(), "preferred_language", owner),
                entity.getExternalId(),
                entity.getExternalUserId());
    }
}
