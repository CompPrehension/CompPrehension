package com.example.demo.Service;

import com.example.demo.models.repository.AnswerObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnswerObjectService {
    private AnswerObjectRepository answerObjectRepository;

    @Autowired
    public AnswerObjectService(AnswerObjectRepository answerObjectRepository) {
        this.answerObjectRepository = answerObjectRepository;
    }
}
