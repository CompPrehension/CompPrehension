package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.Domain;
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

    public Concept getConcept(String name, Domain domain) {
        if (conceptRepository.existsById(name)) {
            return conceptRepository.findById(name).get();
        } else {
            Concept concept = new Concept();
            concept.setName(name);
            concept.setDomain(domain);
            conceptRepository.save(concept);
            return concept;
        }
    }
}
