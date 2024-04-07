package org.vstu.compprehension.models.businesslogic;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class Tag {
    String name;
    /** ID-like bit of the concept for a bitmask combining several Concepts */
    long bitmask;

    public static long combineToBitmask(@Nullable Iterable<Tag> tags) {
        if (tags == null)
            return 0;

        long result = 0;
        for (Tag t : tags) {
            long newBit = t.getBitmask();
            result |= newBit;
        }
        return result;
    }
}
