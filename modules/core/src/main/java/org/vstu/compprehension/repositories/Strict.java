package org.vstu.compprehension.repositories;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Чтение обязательных полей персистентных типов.
 * <p>
 * Живёт в корне слоя доступа к данным, потому что пользуются им и репозитории,
 * и мапперы из {@code repositories.mappers}.
 */
public final class Strict {

    private Strict() {
    }

    /** Значение обязательного поля; иначе — исключение с указанием поля и владельца. */
    public static <T> @NotNull T required(@Nullable T value, @NotNull String field, @NotNull Object ownerId) {
        if (value == null) {
            throw new IllegalStateException(
                    "Field '" + field + "' of " + ownerId + " is null or was not loaded");
        }
        return value;
    }
}
