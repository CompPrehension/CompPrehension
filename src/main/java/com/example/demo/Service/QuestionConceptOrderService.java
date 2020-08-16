package com.example.demo.Service;

import com.example.demo.models.repository.QuestionConceptOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionConceptOrderService {
    private QuestionConceptOrderRepository questionConceptOrderRepository;

    @Autowired
    public QuestionConceptOrderService(QuestionConceptOrderRepository questionConceptOrderRepository) {
        this.questionConceptOrderRepository = questionConceptOrderRepository;
    }
}
