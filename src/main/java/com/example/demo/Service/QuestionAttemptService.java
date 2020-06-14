package com.example.demo.Service;

import com.example.demo.models.Dao.QuestionAttemptDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionAttemptService {
    private QuestionAttemptDao questionAttemptDao;

    @Autowired
    public QuestionAttemptService(QuestionAttemptDao questionAttemptDao) {
        this.questionAttemptDao = questionAttemptDao;
    }
}
