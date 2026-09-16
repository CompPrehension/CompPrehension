package org.vstu.compprehension.businesslogic;

import java.util.Map;

public record Laws(Map<String, PositiveLaw> positive, Map<String, NegativeLaw> negative) {
    private static final Laws EMPTY = new Laws(Map.of(), Map.of());

    public static Laws empty() {
        return EMPTY;
    }
}
