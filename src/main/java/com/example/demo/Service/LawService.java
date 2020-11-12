package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.entities.Law;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LawService {

    public Law getLaw(String name, boolean isPositive, DomainEntity domainEntity, List<Concept> concepts) {
        Law law = new Law();
        law.setName(name);
        law.setDomain(domainEntity);
        law.setConcepts(concepts);
        return law;
    }
}
