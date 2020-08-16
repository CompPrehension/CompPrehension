package com.example.demo.Service;

import com.example.demo.models.repository.QuestionConceptMatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionConceptMatchService {
    private QuestionConceptMatchRepository questionConceptMatchRepository;

    @Autowired
    public QuestionConceptMatchService(QuestionConceptMatchRepository questionConceptMatchRepository) {
        this.questionConceptMatchRepository = questionConceptMatchRepository;
    }
}
