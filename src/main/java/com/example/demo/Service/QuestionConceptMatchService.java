package com.example.demo.Service;

import com.example.demo.models.Dao.QuestionConceptMatchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionConceptMatchService {
    private QuestionConceptMatchDao questionConceptMatchDao;

    @Autowired
    public QuestionConceptMatchService(QuestionConceptMatchDao questionConceptMatchDao) {
        this.questionConceptMatchDao = questionConceptMatchDao;
    }
}
