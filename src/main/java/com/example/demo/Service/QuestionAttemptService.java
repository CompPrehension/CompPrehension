package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.CourseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.Dao.QuestionAttemptDao;
import com.example.demo.models.entities.Question;
import com.example.demo.models.entities.QuestionAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionAttemptService {
    private QuestionAttemptDao questionAttemptDao;

    @Autowired
    public QuestionAttemptService(QuestionAttemptDao questionAttemptDao) {
        this.questionAttemptDao = questionAttemptDao;
    }
    
    public void saveQuestionAttempt(QuestionAttempt questionAttempt) {
        
        questionAttemptDao.save(questionAttempt);
    }
    
    public QuestionAttempt getQuestionAttempt(Long questionAttemptId) {
        try {
            return questionAttemptDao.findById(questionAttemptId).orElseThrow(()->
                    new CourseNFException("QuestionAttempt with id: " +
                            questionAttemptId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-course to Model-course", e);
        }
    }
    
}

