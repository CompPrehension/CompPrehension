package org.vstu.compprehension.businesslogic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Concept implements TreeNodeWithBitmask {
    /** When present, this flag enables a concept to be shown to teacher at exercise configuration page. */
    public static final int FLAG_VISIBLE_TO_TEACHER = 1;
    /** When present, this flag enables a concept to be selected as TARGET at exercise configuration page. */
    public static final int FLAG_TARGET_ENABLED = 2;

    /** All flags are OFF by default */
    public static final int DEFAULT_FLAGS = 0;

    @EqualsAndHashCode.Include
    @ToString.Include
    private final String name;
    @ToString.Include
    private final int bitflags;
    @ToString.Include
    private final long bitmask;
    private final List<Concept> baseConcepts;
    private final int sortOrder = 999;

    private Set<Concept> childConcepts = Set.of();
    @Getter(AccessLevel.NONE)
    private Long subTreeBitmaskCache = null;

    public Concept(String name) {
        this(name, List.of(), DEFAULT_FLAGS, 0L);
    }

    public Concept(String name, int bitflags) {
        this(name, List.of(), bitflags, 0L);
    }

    public Concept(String name, List<Concept> baseConcepts) {
        this(name, baseConcepts, DEFAULT_FLAGS, 0L);
    }

    public Concept(String name, List<Concept> baseConcepts, int bitflags) {
        this(name, baseConcepts, bitflags, 0L);
    }

    public Concept(String name, List<Concept> baseConcepts, int bitflags, long bitmask) {
        this.name = name;
        this.bitflags = bitflags;
        this.bitmask = bitmask;
        this.baseConcepts = List.copyOf(baseConcepts);
    }

    void setChildConcepts(Set<Concept> childConcepts) {
        this.childConcepts = Set.copyOf(childConcepts);
        this.subTreeBitmaskCache = null;
    }

    public boolean hasBaseConcept(Concept concept) {
        if (baseConcepts.contains(concept)) {
            return true;
        }
        for (Concept baseConcept : baseConcepts) {
            if (baseConcept.hasBaseConcept(concept)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFlag(int flagCode) {
    	return (bitflags & flagCode) != 0;
    }

    /**
     * @return bits of this concept and all childConcepts
     */
    public long getSubTreeBitmask() {
        if (subTreeBitmaskCache != null)
            return subTreeBitmaskCache;

        return subTreeBitmaskCache =
                bitmask | (childConcepts.stream().map(Concept::getSubTreeBitmask).reduce((a, b) -> a|b).orElse(0L));
    }

    /**
     * @return set of child concepts (recursively)
     */
    public Set<Concept> getDescendants() {
        Set<Concept> set = new HashSet<>(childConcepts);
        for (Concept childConcept : childConcepts) {
            set.addAll(childConcept.getDescendants());
        }
        return set;
    }

    public static long combineToBitmask(Iterable<Concept> concepts) {
        if (concepts == null)
            return 0;

        long conceptBitmask = 0;
        for (Concept t : concepts) {
            long newBit = t.getBitmask();
            if (newBit == 0) {
                // make use of children
                newBit = t.getSubTreeBitmask();
            }
            conceptBitmask |= newBit;
        }
        return conceptBitmask;
    }
}
