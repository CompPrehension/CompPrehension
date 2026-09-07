package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.data.enums.Language;

/**
 * Текущий пользователь в объёме, нужном вызывающему коду.
 * <p>
 * Заменяет {@code UserEntity}, которую {@code UserService} отдавал наружу: контроллерам
 * от пользователя нужны идентификатор, язык и пара полей для карточки, а сущность тянула
 * за собой и ленивые связи, и зависимость от слоя хранения.
 */
public record CurrentUserData(
        long id,
        @Nullable String firstName,
        @Nullable String lastName,
        @Nullable String email,
        @NotNull Language language) {
}
