package org.vstu.compprehension.businesslogic.domains;

import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Laws;
import org.vstu.compprehension.businesslogic.Skill;

import java.util.Map;

public record DomainStructure(Map<String, Concept> concepts, Map<String, Skill> skills, Laws laws) {
    public DomainStructure withSkills(Map<String, Skill> skills) {
        return new DomainStructure(concepts, skills, laws);
    }
}
