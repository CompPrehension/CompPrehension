package org.vstu.compprehension.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
