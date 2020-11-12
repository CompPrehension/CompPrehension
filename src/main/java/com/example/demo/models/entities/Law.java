package com.example.demo.models.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class Law {
    private String name;
    private boolean isPositiveLaw;
    private List<LawFormulation> lawFormulations;
    private DomainEntity domain;
    private List<Concept> concepts;
    private List<Tag> tags;

    public static Law createLaw(String name, boolean isPositive, DomainEntity domainEntity, List<Concept> concepts) {
        Law law = new Law();
        law.setName(name);
        law.setPositiveLaw(isPositive);
        law.setDomain(domainEntity);
        law.setConcepts(concepts);
        return law;
    }
}
