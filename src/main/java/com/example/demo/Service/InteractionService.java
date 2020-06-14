package com.example.demo.Service;

import com.example.demo.models.Dao.InteractionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InteractionService {
    private InteractionDao interactionDao;

    @Autowired
    public InteractionService(InteractionDao interactionDao) {
        this.interactionDao = interactionDao;
    }
}
