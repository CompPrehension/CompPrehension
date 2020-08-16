package com.example.demo.Service;

import com.example.demo.models.repository.LawFormulationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LawFormulationService {
    private LawFormulationRepository lawFormulationRepository;

    @Autowired
    public LawFormulationService(LawFormulationRepository lawFormulationRepository) {
        this.lawFormulationRepository = lawFormulationRepository;
    }
}
