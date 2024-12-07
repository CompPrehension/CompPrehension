package org.vstu.compprehension.models.businesslogic;

import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Tag: a label to mark a feature, or a subset of domain definitions
 * having e.g. common purpose.
 * Tags can form a hierarchy via `implies` property.
 */
@Value
@AllArgsConstructor
public class Tag {
    /** Unique id-like name of the Tag. Can be used as localization key. */
    String name;

    /** ID-like bit of the concept for a bitmask combining several Concepts */
    @Getter
    long bitmask;

    /** Optional base Tag. 
     * If set, using domain definitions marked by this tag, 
     * definitions by base Tag are assumed to be used as well.
     * This should be checked transitively.
     *  */
//    @Builder.Default
    @Getter
    Tag implies;

    /** Short variant of constructor */
    public Tag(String name, long bitmask) {
        this(name, bitmask, null);
    }

    /** Get all implied tags, including this tag. */
    public List<Tag> withAllImplied() {
        List<Tag> list;

        if (implies == null) {
            list = new ArrayList<>();  // base of recursion
        } else {
            list = implies.withAllImplied();  // step of recursion
        }
        list.add(this);
        return list;
    }

    public long bitmaskWithImplied() {
        return combineToBitmask(withAllImplied());
    }

    /** Obtain a bitmask having all bits from given tags. */
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
