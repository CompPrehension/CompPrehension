package org.vstu.compprehension.models.businesslogic;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class Concept {
    String name;
    List<Concept> baseConcepts;

    public Concept(String name) {
        this.name = name;
        this.baseConcepts = new ArrayList<>();
    }

    public Concept(String name, List<Concept> baseConcepts) {
        this.name = name;
        this.baseConcepts = baseConcepts;
    }

    public boolean hasBaseConcept(Concept concept) {
        if (baseConcepts.contains(concept)) {
            return true;
        } else {
            for (Concept baseConcept : baseConcepts) {
                if (baseConcept.hasBaseConcept(concept)) {
                    return true;
                }
            }
        }
        return false;
    }
}
