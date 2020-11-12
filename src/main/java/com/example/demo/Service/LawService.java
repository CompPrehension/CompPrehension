package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.entities.Law;
import gnu.trove.map.hash.THashMap;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LawService {

    THashMap<String, Law> laws;

    public Law createLaw(String name, boolean isPositive, DomainEntity domainEntity, List<Concept> concepts) {
        if (laws.contains(name)) {
            return laws.get(name);
        }
        Law law = new Law();
        law.setName(name);
        law.setPositiveLaw(isPositive);
        law.setDomain(domainEntity);
        law.setConcepts(concepts);
        laws.put(name, law);
        return law;
    }

    public Optional<Law> getLaw(String name) {
        if (laws.containsKey(name)) {
            return Optional.of(laws.get(name));
        } else {
            return Optional.empty();
        }
    }
}
