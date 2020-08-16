package com.example.demo.Service;

import com.example.demo.models.repository.InteractionRepository;
import com.example.demo.models.entities.Interaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InteractionService {
    private InteractionRepository interactionRepository;

    @Autowired
    public InteractionService(InteractionRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }
    
    public void saveInteraction(Interaction interaction) {
        
        interactionRepository.save(interaction);
    }
}
