package org.vstu.compprehension.businesslogic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Set;

public final class ConceptsBuilder {
    private final LinkedHashMap<String, Concept> concepts = new LinkedHashMap<>();

    public Concept add(String name) {
        return add(new Concept(name));
    }

    public Concept add(String name, List<Concept> bases) {
        return add(new Concept(name, bases));
    }

    public Concept add(String name, List<Concept> bases, Set<DomainItemFlag> flags) {
        return add(new Concept(name, bases, flags));
    }

    public Concept add(String name, long bit, List<Concept> bases, Set<DomainItemFlag> flags) {
        return add(new Concept(name, bases, flags, bit));
    }

    public Concept add(Concept concept) {
        if (concepts.putIfAbsent(concept.getName(), concept) != null) {
            throw new IllegalArgumentException("Duplicate concept: " + concept.getName());
        }
        return concept;
    }

    public ConceptsBuilder addAll(Collection<Concept> all) {
        all.forEach(this::add);
        return this;
    }

    public Concept get(String name) {
        Concept concept = concepts.get(name);
        if (concept == null) {
            throw new IllegalArgumentException("Unknown concept: " + name);
        }
        return concept;
    }

    public Map<String, Concept> build() {
        Map<Concept, Set<Concept>> children = new HashMap<>();
        for (Concept concept : concepts.values()) {
            for (Concept base : concept.getBaseConcepts()) {
                if (concepts.get(base.getName()) != base) {
                    throw new IllegalStateException("Concept " + concept.getName() + " has unknown base " + base.getName());
                }
                children.computeIfAbsent(base, k -> new HashSet<>()).add(concept);
            }
        }
        children.forEach(Concept::setChildConcepts);
        return Collections.unmodifiableMap(new LinkedHashMap<>(concepts));
    }
}
