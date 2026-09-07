package org.vstu.compprehension.data.enums;

public enum SearchDirections {
    TO_SIMPLE(-1),
    TO_COMPLEX(1);

    private final int value;
    private SearchDirections(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
