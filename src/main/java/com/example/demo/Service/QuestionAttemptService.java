package com.example.demo.Service;

import com.example.demo.Exceptions.NotFoundEx.CourseNFException;
import com.example.demo.Exceptions.NotFoundEx.UserNFException;
import com.example.demo.models.repository.QuestionAttemptRepository;
import com.example.demo.models.entities.QuestionAttemptEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionAttemptService {
    private QuestionAttemptRepository questionAttemptRepository;

    @Autowired
    public QuestionAttemptService(QuestionAttemptRepository questionAttemptRepository) {
        this.questionAttemptRepository = questionAttemptRepository;
    }
    
    public void saveQuestionAttempt(QuestionAttemptEntity questionAttempt) {
        
        questionAttemptRepository.save(questionAttempt);
    }
    
    public QuestionAttemptEntity getQuestionAttempt(Long questionAttemptId) {
        try {
            return questionAttemptRepository.findById(questionAttemptId).orElseThrow(()->
                    new CourseNFException("QuestionAttempt with id: " +
                            questionAttemptId + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-course to Model-course", e);
        }
    }
    
}

