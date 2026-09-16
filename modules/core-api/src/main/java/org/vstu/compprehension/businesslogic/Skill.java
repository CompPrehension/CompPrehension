package org.vstu.compprehension.businesslogic;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Skill implements TreeNodeWithBitmask {
    /** When present, this flag enables a concept to be shown to teacher at exercise configuration page. */
    public static final int FLAG_VISIBLE_TO_TEACHER = 1;
    /** When present, this flag blocks selection this skill as denied */
    // TODO: implement in frontend
    public static final int FLAG_DENIED_DISABLED = 2;

    /** All flags are OFF by default */
    public static final int DEFAULT_FLAGS = 0;

    @EqualsAndHashCode.Include
    @ToString.Include
    private final String name;
    @ToString.Include
    private final int bitflags;
    @ToString.Include
    private final long bitmask;
    private final List<Skill> baseSkills;
    private final int sortOrder = 999;

    private Set<Skill> childSkills = Set.of();
    @Getter(AccessLevel.NONE)
    private Long subTreeBitmaskCache = null;

    public Skill(String name, long bitmask) {
        this(name, List.of(), DEFAULT_FLAGS, bitmask);
    }

    public Skill(String name, List<Skill> baseSkills, long bitmask) {
        this(name, baseSkills, DEFAULT_FLAGS, bitmask);
    }

    public Skill(String name, List<Skill> baseSkills, int bitflags, long bitmask) {
        this.name = name;
        this.bitflags = bitflags;
        this.bitmask = bitmask;
        this.baseSkills = List.copyOf(baseSkills);
    }

    void setChildSkills(Set<Skill> childSkills) {
        this.childSkills = Set.copyOf(childSkills);
        this.subTreeBitmaskCache = null;
    }

    public boolean hasFlag(int flagCode) {
        return (bitflags & flagCode) != 0;
    }

    public static long combineToBitmask(List<Skill> targetSkills) {
        if (targetSkills == null)
            return 0;

        long skillBitmask = 0;
        for (Skill t : targetSkills) {
            long newBit = t.getBitmask();
            if (newBit == 0) {
                // make use of children
                newBit = t.getSubTreeBitmask();
            }
            skillBitmask |= newBit;
        }
        return skillBitmask;
    }

    public boolean hasBaseSkill(Skill skill) {
        if (baseSkills.contains(skill)) {
            return true;
        }
        for (Skill baseSkill : baseSkills) {
            if (baseSkill.hasBaseSkill(skill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return bits of this concept and all childConcepts
     */
    public long getSubTreeBitmask() {
        if (subTreeBitmaskCache != null)
            return subTreeBitmaskCache;

        return subTreeBitmaskCache =
                bitmask | (childSkills.stream().map(Skill::getSubTreeBitmask).reduce((a, b) -> a|b).orElse(0L));
    }

    /**
     * @return set of child concepts (recursively)
     */
    public Set<Skill> getDescendants() {
        Set<Skill> set = new HashSet<>(childSkills);
        for (Skill childSkill : childSkills) {
            set.addAll(childSkill.getDescendants());
        }
        return set;
    }

    /**
     * Recursively searches for the closest ancestors with the {@code FLAG_VISIBLE_TO_TEACHER} flag.
     * @return A set of the closest visible ancestors (maybe empty)
     */
    public Set<Skill> getClosestVisibleParents() {
        if (this.hasFlag(Skill.FLAG_VISIBLE_TO_TEACHER)) {
            return Set.of(this);
        }

        return this.baseSkills.stream()
                .flatMap(bs -> bs.getClosestVisibleParents().stream())
                .collect(Collectors.toSet());
    }
}
