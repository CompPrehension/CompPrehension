package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.Domain;
import com.example.demo.models.entities.Law;
import com.example.demo.models.repository.LawRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LawService {
    private LawRepository lawRepository;

    @Autowired
    public LawService(LawRepository lawRepository) {
        this.lawRepository = lawRepository;
    }

    public Law getLaw(String name, boolean isPositive, Domain domain, List<Concept> concepts) {
        if (lawRepository.existsById(name)) {
            return lawRepository.findById(name).get();
        } else {
            Law law = new Law();
            law.setName(name);
            law.setDomain(domain);
            law.setConcepts(concepts);
            lawRepository.save(law);
            return law;
        }
    }
}
