package org.vstu.compprehension.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Utils {
    public static <P, C extends P> Optional<C> tryCast(@Nullable P obj, @NotNull Class<C> target) {
        if (obj != null && target.isAssignableFrom(obj.getClass())) {
            //noinspection unchecked
            return Optional.of((C)obj);
        }
        return Optional.empty();
    }

    public static <T> Map<T, Integer> countElements(Collection<T> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        item -> item,
                        item -> 1,
                        Integer::sum
                ));
    }
}
