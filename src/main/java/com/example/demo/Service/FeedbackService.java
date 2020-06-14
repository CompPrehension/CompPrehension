package com.example.demo.Service;

import com.example.demo.models.Dao.FeedbackDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
    private FeedbackDao feedbackDao;

    @Autowired
    public FeedbackService(FeedbackDao feedbackDao) {
        this.feedbackDao = feedbackDao;
    }
}
