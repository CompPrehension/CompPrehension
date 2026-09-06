package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.Language;

/**
 * Учётная запись, какой она лежит в базе.
 * <p>
 * Отличается от {@link CurrentUserData} назначением: та описывает, кого показать
 * в интерфейсе, а эта — что сейчас записано, чтобы решить, что писать поверх. Именно
 * поэтому здесь есть внешние идентификаторы, которых в карточке пользователя нет и
 * быть не должно.
 *
 * @param externalId     идентификатор в системе аутентификации (Keycloak)
 * @param externalUserId идентификатор в LMS ({@code sub} из LTI-токена); по нему уходит
 *                       оценка обратно в LMS
 */
public record UserAccountData(
        long id,
        @Nullable String firstName,
        @Nullable String lastName,
        @NotNull String email,
        @NotNull Language language,
        @Nullable String externalId,
        @Nullable String externalUserId) {
}
