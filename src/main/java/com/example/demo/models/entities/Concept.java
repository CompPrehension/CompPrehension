package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Concept {
    private String name;
    private DomainEntity domain;
    private List<Law> laws;

    public static Concept createConcept(String name, DomainEntity domainEntity) {
        Concept concept = new Concept();
        concept.setName(name);
        concept.setDomain(domainEntity);
        return concept;
    }
}
