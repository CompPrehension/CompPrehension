package org.vstu.compprehension.businesslogic;

import java.util.EnumSet;
import java.util.Set;

public enum DomainItemFlag {
    VISIBLE_TO_TEACHER(1),
    TARGET_ENABLED(2);

    private final int value;

    DomainItemFlag(int value) {
        this.value = value;
    }

    public static Set<DomainItemFlag> fromBitflags(int bitflags) {
        var flags = EnumSet.noneOf(DomainItemFlag.class);
        for (var flag : values()) {
            if ((bitflags & flag.value) != 0) {
                flags.add(flag);
            }
        }
        return flags;
    }
}
