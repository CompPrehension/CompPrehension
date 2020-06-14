package com.example.demo.models.businesslogic.frontend;

import com.example.demo.models.entities.AnswerObject;
import com.example.demo.models.entities.EnumData.QuestionType;
import com.example.demo.utils.HyperText;

import java.util.List;

public interface QuestionFront {
    
    public List<AnswerObject> getAnswerObjects();
    
    public HyperText getQuestionText();
    
    public AnswerObject getAnswerObject(int index);

    public int AnswerObjectsCount();
    
    public QuestionType getQuestionType();
}
