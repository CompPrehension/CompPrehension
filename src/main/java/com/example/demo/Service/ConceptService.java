package com.example.demo.Service;

import com.example.demo.models.repository.ConceptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {
    private ConceptRepository conceptDao;

    @Autowired
    public ConceptService(ConceptRepository conceptDao) {
        this.conceptDao = conceptDao;
    }
}
