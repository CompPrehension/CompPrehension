package com.example.demo.models.businesslogic.frontend;

import com.example.demo.models.entities.AnswerObjectEntity;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.utils.HyperText;

import java.util.List;

public interface QuestionFront {
    
    public List<AnswerObjectEntity> getAnswerObjects();
    
    public HyperText getQuestionText();
    
    public AnswerObjectEntity getAnswerObject(int index);

    public int answerObjectsCount();
    
    public QuestionType getQuestionType();
    
    public Long getExerciseAttemptId();
    
    
}
