package org.vstu.compprehension.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Utils {
    public static <P, C extends P> Optional<C> tryCast(@Nullable P obj, @NotNull Class<C> target) {
        if (obj != null && target.isAssignableFrom(obj.getClass())) {
            //noinspection unchecked
            return Optional.of((C)obj);
        }
        return Optional.empty();
    }
}
