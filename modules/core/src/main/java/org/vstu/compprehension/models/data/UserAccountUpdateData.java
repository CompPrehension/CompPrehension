package org.vstu.compprehension.models.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.entities.EnumData.Language;

/**
 * Состояние учётной записи после входа: то, что нужно записать целиком.
 * <p>
 * Перечислены все поля, которые вход определяет, и записываются они безусловно — иначе
 * «не трогать поле, если пришёл null» превращает запись в набор частных случаев,
 * различить которые по сигнатуре невозможно. Что подставить вместо отсутствующего
 * значения — например, оставить прежний язык или взять язык по умолчанию — решает
 * вызывающий, потому что это зависит от способа входа.
 *
 * @param email опознаётся запись именно по нему
 */
public record UserAccountUpdateData(
        @NotNull String email,
        @Nullable String fullName,
        @NotNull Language language,
        @Nullable String externalId,
        @Nullable String externalUserId) {
}
