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
    @EqualsAndHashCode.Include
    @ToString.Include
    private final String name;
    @ToString.Include
    private final Set<DomainItemFlag> flags;
    @ToString.Include
    private final long bitmask;
    private final List<Concept> baseConcepts;
    private final int sortOrder = 999;

    private Set<Concept> childConcepts = Set.of();
    @Getter(AccessLevel.NONE)
    private Long subTreeBitmaskCache = null;

    public Concept(String name) {
        this(name, List.of(), Set.of(), 0L);
    }

    public Concept(String name, Set<DomainItemFlag> flags) {
        this(name, List.of(), flags, 0L);
    }

    public Concept(String name, List<Concept> baseConcepts) {
        this(name, baseConcepts, Set.of(), 0L);
    }

    public Concept(String name, List<Concept> baseConcepts, Set<DomainItemFlag> flags) {
        this(name, baseConcepts, flags, 0L);
    }

    public Concept(String name, List<Concept> baseConcepts, Set<DomainItemFlag> flags, long bitmask) {
        this.name = name;
        this.flags = Set.copyOf(flags);
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

    public boolean hasFlag(DomainItemFlag flag) {
        return flags.contains(flag);
    }

    public boolean hasFlags(DomainItemFlag... requiredFlags) {
        for (var flag : requiredFlags) {
            if (!flags.contains(flag)) {
                return false;
            }
        }
        return true;
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
