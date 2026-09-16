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
    @EqualsAndHashCode.Include
    @ToString.Include
    private final String name;
    @ToString.Include
    private final Set<DomainItemFlag> flags;
    @ToString.Include
    private final long bitmask;
    private final List<Skill> baseSkills;
    private final int sortOrder = 999;

    private Set<Skill> childSkills = Set.of();
    @Getter(AccessLevel.NONE)
    private Long subTreeBitmaskCache = null;

    public Skill(String name, long bitmask) {
        this(name, List.of(), Set.of(), bitmask);
    }

    public Skill(String name, List<Skill> baseSkills, long bitmask) {
        this(name, baseSkills, Set.of(), bitmask);
    }

    public Skill(String name, List<Skill> baseSkills, Set<DomainItemFlag> flags, long bitmask) {
        this.name = name;
        this.flags = Set.copyOf(flags);
        this.bitmask = bitmask;
        this.baseSkills = List.copyOf(baseSkills);
    }

    void setChildSkills(Set<Skill> childSkills) {
        this.childSkills = Set.copyOf(childSkills);
        this.subTreeBitmaskCache = null;
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
     * Recursively searches for the closest ancestors with the {@code VISIBLE_TO_TEACHER} flag.
     * @return A set of the closest visible ancestors (maybe empty)
     */
    public Set<Skill> getClosestVisibleParents() {
        if (this.hasFlag(DomainItemFlag.VISIBLE_TO_TEACHER)) {
            return Set.of(this);
        }

        return this.baseSkills.stream()
                .flatMap(bs -> bs.getClosestVisibleParents().stream())
                .collect(Collectors.toSet());
    }
}
