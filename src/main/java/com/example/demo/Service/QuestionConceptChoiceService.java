package com.example.demo.Service;

import com.example.demo.models.repository.QuestionConceptChoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionConceptChoiceService {
    private QuestionConceptChoiceRepository questionConceptChoiceRepository;

    @Autowired
    public QuestionConceptChoiceService(QuestionConceptChoiceRepository questionConceptChoiceRepository) {
        this.questionConceptChoiceRepository = questionConceptChoiceRepository;
    }
}
