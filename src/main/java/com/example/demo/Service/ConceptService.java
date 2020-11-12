package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.entities.Law;
import gnu.trove.map.hash.THashMap;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConceptService {

    THashMap<String, Concept> concepts;

    public Concept createConcept(String name, DomainEntity domainEntity) {
        if (concepts.contains(name)) {
            return concepts.get(name);
        }
        Concept concept = new Concept();
        concept.setName(name);
        concept.setDomain(domainEntity);
        concepts.put(name, concept);
        return concept;
    }

    public Optional<Concept> getConcept(String name) {
        if (concepts.containsKey(name)) {
            return Optional.of(concepts.get(name));
        } else {
            return Optional.empty();
        }
    }
}
