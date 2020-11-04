package com.example.demo.Service;

import com.example.demo.models.entities.Concept;
import com.example.demo.models.entities.DomainEntity;
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

    public Law getLaw(String name, boolean isPositive, DomainEntity domainEntity, List<Concept> concepts) {
        if (lawRepository.existsById(name)) {
            return lawRepository.findById(name).get();
        } else {
            Law law = new Law();
            law.setName(name);
            law.setDomain(domainEntity);
            law.setConcepts(concepts);
            lawRepository.save(law);
            return law;
        }
    }
}
