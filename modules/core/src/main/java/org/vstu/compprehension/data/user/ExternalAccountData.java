package org.vstu.compprehension.data.user;

import org.jetbrains.annotations.NotNull;

/**
 * Привязка учётной записи к внешней системе.
 * <p>
 * Из связи читается ровно это: кто у нас, кто он там и в какой системе. Остальные её
 * поля — те же самые пользователь и ресурс, только ссылками.
 *
 * @param externalId идентификатор пользователя во внешней системе; её формат — её дело,
 *                   поэтому строка
 */
public record ExternalAccountData(long userId, long educationResourceId, @NotNull String externalId) {
}
