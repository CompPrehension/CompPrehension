package org.vstu.compprehension.models.businesslogic;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Concept {
    /** When present, this flag enables a concept to be shown to teacher at exercise configuration page. */
    public static int FLAG_VISIBLE_TO_TEACHER = 1;
    /** When present, this flag enables a concept to be selected as TARGET at exercise configuration page. */
    public static int FLAG_TARGET_ENABLED = 2;

    /** All flags are OFF by default */
    public static int DEFAULT_FLAGS = 0;

    @EqualsAndHashCode.Include
    String name;
    int bitflags;
    List<Concept> baseConcepts;

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
}
