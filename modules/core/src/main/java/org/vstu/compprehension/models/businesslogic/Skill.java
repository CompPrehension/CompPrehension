package org.vstu.compprehension.models.businesslogic;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringExclude;

import java.util.*;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Skill implements TreeNodeWithBitmask {
    @EqualsAndHashCode.Include
    public String name;

    @Setter
    public long bitmask;

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
    }

    public Skill(String name, List<Skill> baseSkills) {
        this.name = name;
        this.baseSkills = new ArrayList<>(baseSkills);
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
