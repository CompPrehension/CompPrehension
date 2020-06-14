package com.example.demo.Service;

import com.example.demo.models.Dao.QuestionConceptChoiceDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionConceptChoiceService {
    private QuestionConceptChoiceDao questionConceptChoiceDao;

    @Autowired
    public QuestionConceptChoiceService(QuestionConceptChoiceDao questionConceptChoiceDao) {
        this.questionConceptChoiceDao = questionConceptChoiceDao;
    }
}
