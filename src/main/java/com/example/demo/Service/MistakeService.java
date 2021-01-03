package com.example.demo.Service;

import com.example.demo.models.repository.MistakeRepository;
import com.example.demo.models.entities.MistakeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MistakeService {
    private MistakeRepository mistakeRepository;

    @Autowired
    public MistakeService(MistakeRepository mistakeRepository) {
        this.mistakeRepository = mistakeRepository;
    }
    
    public void saveMistake(MistakeEntity mistake) {
        
        mistakeRepository.save(mistake);
    }
}
