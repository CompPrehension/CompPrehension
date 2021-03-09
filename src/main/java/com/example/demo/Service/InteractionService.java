package com.example.demo.Service;

import com.example.demo.models.repository.InteractionRepository;
import com.example.demo.models.entities.InteractionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InteractionService {
    private InteractionRepository interactionRepository;

    @Autowired
    public InteractionService(InteractionRepository interactionRepository) {
        this.interactionRepository = interactionRepository;
    }

    public void saveInteraction(InteractionEntity interaction) {

        interactionRepository.save(interaction);
    }

    public Iterable<InteractionEntity> getAllInteractions() {

        return interactionRepository.findAll();
    }

    public Optional<InteractionEntity> getInteraction(Long id) {

        return interactionRepository.findById(id);
    }
}
