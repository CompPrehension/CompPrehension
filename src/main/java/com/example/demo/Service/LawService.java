package com.example.demo.Service;

import com.example.demo.models.repository.LawRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LawService {
    private LawRepository lawRepository;

    @Autowired
    public LawService(LawRepository lawRepository) {
        this.lawRepository = lawRepository;
    }
}
