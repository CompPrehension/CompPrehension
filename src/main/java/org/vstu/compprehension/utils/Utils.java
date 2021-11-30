package org.vstu.compprehension.utils;

import java.util.Optional;

public class Utils {
    public static <P, C extends P> Optional<C> tryCast(P obj, Class<C> target) {
        if (target.isAssignableFrom(obj.getClass())) {
            return Optional.ofNullable((C)obj);
        }
        return Optional.empty();
    }
}
