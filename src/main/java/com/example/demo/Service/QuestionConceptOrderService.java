package com.example.demo.Service;

import com.example.demo.models.Dao.QuestionConceptOrderDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionConceptOrderService {
    private QuestionConceptOrderDao questionConceptOrderDao;

    @Autowired
    public QuestionConceptOrderService(QuestionConceptOrderDao questionConceptOrderDao) {
        this.questionConceptOrderDao = questionConceptOrderDao;
    }
}
