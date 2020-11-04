package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
import com.example.demo.models.repository.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {
    private ConceptRepository conceptRepository;

    @Autowired
    public ConceptService(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    public Concept getConcept(String name, DomainEntity domainEntity) {
        if (conceptRepository.existsById(name)) {
            return conceptRepository.findById(name).get();
        } else {
            Concept concept = new Concept();
            concept.setName(name);
            concept.setDomain(domainEntity);
            conceptRepository.save(concept);
            return concept;
        }
    }
}
