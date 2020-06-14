package com.example.demo.Service;

import com.example.demo.models.Dao.ConceptDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptService {
    private ConceptDao conceptDao;

    @Autowired
    public ConceptService(ConceptDao conceptDao) {
        this.conceptDao = conceptDao;
    }
}
