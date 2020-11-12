package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {

    public Concept getConcept(String name, DomainEntity domainEntity) {
        Concept concept = new Concept();
        concept.setName(name);
        concept.setDomain(domainEntity);
        return concept;
    }
}
