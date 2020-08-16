package com.example.demo.Service;


import com.example.demo.models.repository.AdditionalFieldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdditionalFieldService {
    private AdditionalFieldRepository additionalFieldRepository;

    @Autowired
    public AdditionalFieldService(AdditionalFieldRepository additionalFieldRepository) {
        this.additionalFieldRepository = additionalFieldRepository;
    }
}
