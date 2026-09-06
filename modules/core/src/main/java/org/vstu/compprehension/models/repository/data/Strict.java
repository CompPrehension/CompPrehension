package org.vstu.compprehension.models.repository.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Проверки для тотального маппинга.
 * <p>
 * {@code *Data} обещает, что объявленное поле заполнено. Схема этого не гарантирует:
 * колонка может быть nullable исторически, связь — не попасть в выборку, а строка —
 * исчезнуть между двумя запросами. Тихо подставить {@code null} значит вернуть данные,
 * которые нарушают собственный контракт, и получить NPE в домене — далеко от причины.
 * <p>
 * Поэтому маппинг падает здесь и сообщает, какое именно поле какой записи оказалось
 * пустым. Настоящие «может не быть» — например, попытка вне курса — через эти проверки
 * не проходят: они объявлены {@code @Nullable} в самой {@code *Data}.
 */
final class Strict {

    private Strict() {
    }

    /** Значение обязательного поля; иначе — исключение с указанием поля и владельца. */
    static <T> @NotNull T required(@Nullable T value, @NotNull String field, @NotNull Object ownerId) {
        if (value == null) {
            throw new IllegalStateException(
                    "Field '" + field + "' of " + ownerId + " is null or was not loaded");
        }
        return value;
    }
}
