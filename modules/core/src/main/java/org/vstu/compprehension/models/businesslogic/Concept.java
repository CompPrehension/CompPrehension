package org.vstu.compprehension.models.businesslogic;

import lombok.*;

import java.util.*;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Concept implements TreeNodeWithBitmask {
    /** When present, this flag enables a concept to be shown to teacher at exercise configuration page. */
    public static int FLAG_VISIBLE_TO_TEACHER = 1;
    /** When present, this flag enables a concept to be selected as TARGET at exercise configuration page. */
    public static int FLAG_TARGET_ENABLED = 2;

    /** All flags are OFF by default */
    public static int DEFAULT_FLAGS = 0;

    int bitflags;

    @EqualsAndHashCode.Include
    String name;

    List<Concept> baseConcepts;

    /** Cached references to Concept instances */
    @Getter
    @Setter
    Collection<Concept> childConcepts = null;


    /** ID-like bit of the concept for a bitmask combining several Concepts */
    long bitmask;

    public Concept(String name) {
        this.name = name;
        this.bitflags = DEFAULT_FLAGS;
        this.baseConcepts = new ArrayList<>();
    }

    public Concept(String name, int bitflags) {
        this.name = name;
        this.bitflags = bitflags;
        this.baseConcepts = new ArrayList<>();
    }

    public Concept(String name, List<Concept> baseConcepts) {
        this.name = name;
        this.bitflags = DEFAULT_FLAGS;
        this.baseConcepts = new ArrayList<>(baseConcepts);
    }

    public Concept(String name, List<Concept> baseConcepts, int bitflags) {
        this.name = name;
        this.bitflags = bitflags;
        this.baseConcepts = new ArrayList<>(baseConcepts);
    }

    public boolean hasBaseConcept(Concept concept) {
        if (baseConcepts.contains(concept)) {
            return true;
        } else {
            // search up along the hierarchy
            for (Concept baseConcept : baseConcepts) {
                if (baseConcept.hasBaseConcept(concept)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasFlag(int flagCode) {
    	return (bitflags & flagCode) != 0;
    }

    Long subTreeBitmaskCache = null;

    /**
     * @return bits of this concept and all childConcepts
     */
    public long getSubTreeBitmask() {
        if (childConcepts == null)
            return bitmask;

        if (subTreeBitmaskCache != null)
            return subTreeBitmaskCache;

        return subTreeBitmaskCache =
                bitmask | (childConcepts.stream().map(Concept::getSubTreeBitmask).reduce((a, b) -> a|b).orElse(0L));
    }

    /**
     * @return set of child concepts (recursively)
     */
    public Set<Concept> getDescendants() {
        if (childConcepts == null)
            return Set.of();

        Set<Concept> set = new HashSet<>(childConcepts);
        for (Concept childConcept : childConcepts) {
            set.addAll(childConcept.getDescendants());
        }
        return set;
    }
}
