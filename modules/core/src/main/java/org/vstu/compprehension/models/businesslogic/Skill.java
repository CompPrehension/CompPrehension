package org.vstu.compprehension.models.businesslogic;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringExclude;

import java.util.*;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Skill implements TreeNodeWithBitmask {
    /** When present, this flag enables a concept to be shown to teacher at exercise configuration page. */
    public static int FLAG_VISIBLE_TO_TEACHER = 1;
    /** When present, this flag blocks selection this skill as denied */
    // TODO: implement in frontend
    public static int FLAG_DENIED_DISABLED = 2;

    /** All flags are OFF by default */
    public static int DEFAULT_FLAGS = 0;

    @EqualsAndHashCode.Include
    public String name;

    int bitflags;

    @Setter
    long bitmask;

    @Setter
    int sortOrder = 999;

    @ToStringExclude
    List<Skill> baseSkills;

    /** Cached references to Skill instances */
    @ToStringExclude
    @Setter
    Collection<Skill> childSkills = null;


    public Skill(String name) {
        this.name = name;
        this.baseSkills = new ArrayList<>();
        this.bitflags = DEFAULT_FLAGS;
    }

    public Skill(String name, List<Skill> baseSkills, int bitflags) {
        this.name = name;
        this.bitflags = bitflags;
        this.baseSkills = new ArrayList<>(baseSkills);
        for (Skill base : baseSkills) {
            if (base.childSkills == null) {
                base.childSkills = new ArrayList<>();
            }
            base.childSkills.add(this);
        }
    }

    public Skill(String name, List<Skill> baseSkills) {
        this(name, baseSkills, DEFAULT_FLAGS);
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
        } else {
            // search up along the hierarchy
            for (Skill baseSkill : baseSkills) {
                if (baseSkill.hasBaseSkill(skill)) {
                    return true;
                }
            }
        }
        return false;
    }

    Long subTreeBitmaskCache = null;

    /**
     * @return bits of this concept and all childConcepts
     */
    public long getSubTreeBitmask() {
        if (childSkills == null)
            return bitmask;

        if (subTreeBitmaskCache != null)
            return subTreeBitmaskCache;

        return subTreeBitmaskCache =
                bitmask | (childSkills.stream().map(Skill::getSubTreeBitmask).reduce((a, b) -> a|b).orElse(0L));
    }

    /**
     * @return set of child concepts (recursively)
     */
    public Set<Skill> getDescendants() {
        if (childSkills == null)
            return Set.of();

        Set<Skill> set = new HashSet<>(childSkills);
        for (Skill childSkill : childSkills) {
            set.addAll(childSkill.getDescendants());
        }
        return set;
    }
}
