package org.vstu.compprehension.businesslogic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Set;

public final class SkillsBuilder {
    private final LinkedHashMap<String, Skill> skills = new LinkedHashMap<>();
    private final Map<Long, Skill> byBit = new HashMap<>();

    public Skill add(String name, long bit) {
        return add(new Skill(name, List.of(), Set.of(), bit));
    }

    public Skill add(String name, long bit, Set<DomainItemFlag> flags) {
        return add(new Skill(name, List.of(), flags, bit));
    }

    public Skill add(String name, long bit, List<Skill> bases) {
        return add(new Skill(name, bases, Set.of(), bit));
    }

    public Skill add(String name, long bit, List<Skill> bases, Set<DomainItemFlag> flags) {
        return add(new Skill(name, bases, flags, bit));
    }

    private Skill add(Skill skill) {
        long bit = skill.getBitmask();
        if (bit == 0 || Long.bitCount(bit) != 1) {
            throw new IllegalArgumentException("Skill " + skill.getName() + " must have exactly one bit, got " + Long.toHexString(bit));
        }
        Skill sameBit = byBit.putIfAbsent(bit, skill);
        if (sameBit != null) {
            throw new IllegalArgumentException("Skills " + sameBit.getName() + " and " + skill.getName() + " share bit " + Long.toHexString(bit));
        }
        if (skills.putIfAbsent(skill.getName(), skill) != null) {
            throw new IllegalArgumentException("Duplicate skill: " + skill.getName());
        }
        return skill;
    }

    public Map<String, Skill> build() {
        Map<Skill, Set<Skill>> children = new HashMap<>();
        for (Skill skill : skills.values()) {
            for (Skill base : skill.getBaseSkills()) {
                if (skills.get(base.getName()) != base) {
                    throw new IllegalStateException("Skill " + skill.getName() + " has unknown base " + base.getName());
                }
                children.computeIfAbsent(base, k -> new HashSet<>()).add(skill);
            }
        }
        children.forEach(Skill::setChildSkills);
        return Collections.unmodifiableMap(new LinkedHashMap<>(skills));
    }
}
